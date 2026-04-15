package voxelengine;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.joml.Matrix4f;

public class VoxelEngine {

    private static final String VERTEX_SHADER_SOURCE = """
    #version 330 core
    layout (location = 0) in vec3 aPos;    // vertex position
    layout (location = 1) in vec3 aColor;  // vertex color
    layout (location = 2) in float aAxis;  // face axis for grid lines
    uniform mat4 view;                     // camera view matrix
    uniform mat4 projection;               // perspective projection matrix
    uniform mat4 model;                    // chunk world position offset
    out vec3 vColor;                       // passed to fragment shader
    out vec3 vPos;                         // world position for grid lines
    flat out float vAxis;                  // which axis this face is on
    void main() {
        vColor = aColor;
        vPos = (model * vec4(aPos, 1.0)).xyz;
        vAxis = aAxis;
        gl_Position = projection * view * model * vec4(aPos, 1.0);
    }""";
    private static final String FRAGMENT_SHADER_SOURCE = """
    #version 330 core
    in vec3 vColor;                        // receives color from vertex shader
    in vec3 vPos;                          // world position for grid lines
    flat in float vAxis;                   // which axis this face is on
    uniform bool showQuadEdges;            // wireframe toggle
    out vec4 FragColor;                    // the final pixel color
    void main() {
        // Wireframe mode
        if (showQuadEdges) {
            float near = 0.01;
            float far = 1000.0;
            float z = gl_FragCoord.z;
            float linearDepth = (2.0 * near) / (far + near - z * (far - near));
            float fade = clamp(1.0 - linearDepth * 5.0, 0.0, 1.0);
            vec3 nearColor = vec3(0.2, 1.0, 0.3);
            vec3 farColor = vec3(0.02, 0.05, 0.02);
            vec3 color = mix(farColor, nearColor, fade);
            FragColor = vec4(color, 1.0);
            return;
        }

        // Height-based block color (per voxel, not per quad)
        float y = floor(vPos.y + 0.001);
        vec3 blockColor;
        if (y < 10.0)      blockColor = vec3(0.4, 0.35, 0.3);    // deep stone
        else if (y < 25.0) blockColor = vec3(0.5, 0.5, 0.5);     // stone
        else if (y < 30.0) blockColor = vec3(0.45, 0.3, 0.15);   // dirt
        else if (y < 35.0) blockColor = vec3(0.2, 0.6, 0.15);    // grass
        else if (y < 45.0) blockColor = vec3(0.55, 0.55, 0.55);  // mountain rock
        else               blockColor = vec3(0.9, 0.9, 0.95);    // snow

        // Voxel grid lines (skip the axis the face is aligned to)
        vec3 f = fract(vPos);
        float edge = 0.05;
        bool grid = false;
        if (vAxis != 0.0 && (f.x < edge || f.x > 1.0 - edge)) grid = true;
        if (vAxis != 1.0 && (f.y < edge || f.y > 1.0 - edge)) grid = true;
        if (vAxis != 2.0 && (f.z < edge || f.z > 1.0 - edge)) grid = true;

        // Final color
        if (grid) {
            FragColor = vec4(blockColor * 0.3, 1.0);
        } else {
            FragColor = vec4(blockColor, 1.0);
        }
    }""";

    private long window;   // The window handle
    private Camera camera;
    private Shader shader;

    private double lastFrameTime;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    
    private boolean showQuadEdges = false;
    private boolean mouseCaptured = true;
    
    private World world;
    
    private Frustum frustum = new Frustum();

    public void run() {
        init();
        loop();

        // Cleanup mesh and shader
        for (Chunk chunk : world.getVisibleChunks()) {
            if (chunk.mesh != null) {
                chunk.mesh.cleanup();
            }
        }
        shader.cleanup();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(800, 600, "Voxel Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup key callback for closing window by pressing escape
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
            if (key == GLFW_KEY_F && action == GLFW_RELEASE) {
                showQuadEdges = !showQuadEdges;
            }
            if (key == GLFW_KEY_TAB && action == GLFW_RELEASE) {
                mouseCaptured = !mouseCaptured;
                glfwSetInputMode(window, GLFW_CURSOR,
                        mouseCaptured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
                firstMouse = true;
            }
        });
        
        // Set window resize callback
        glfwSetFramebufferSizeCallback(window, (Win, width, height) -> {
           glViewport(0, 0, width, height);
           camera.updateAspectRation((float) width / height);
        });

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Initialize shader
        this.shader = new Shader(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
        
        // Initialize camera
        this.camera = new Camera(16.0f, 50.0f, 16.0f, -30.0f, -90.0f);

        // Initialize world
        this.world = new World(8);
        
        // Set cursor callback for camera rotation
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (!mouseCaptured) return;
            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
                return;
            }

            float dx = (float) (xpos - lastMouseX);
            float dy = (float) (lastMouseY - ypos);  // inverted: moving mouse up = positive pitch

            lastMouseX = xpos;
            lastMouseY = ypos;

            float sensitivity = 0.1f;
            camera.rotate(dy * sensitivity, dx * sensitivity);
        });
        
        // Hide and lock cursor to window
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // Make the window visible
        glfwShowWindow(window);
        
        // Initialize last frame time
        lastFrameTime = glfwGetTime();
    }

    private void loop() {
        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        // FPS counter variables
        int frameCount = 0;
        double fpsTimer = glfwGetTime();

        // Run the rendering loop until the user has attempted to close
        // the window
        while (!glfwWindowShouldClose(window)) {
            // Delta time
            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;
            
            // Camera input
            if (mouseCaptured) {
                float speed = 5.0f * deltaTime;
                float forward = 0.0f, right = 0.0f, up = 0.0f;

                if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) { forward += speed; }
                if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) { forward -= speed; }
                if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) { right -= speed; }
                if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) { right += speed; }
                if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) { up += speed; }
                if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) { up -= speed; }

                camera.move(forward, right, up);
            }
            
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Set wireframe on / off
            if (showQuadEdges) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            } else {
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }
            
            // Update world based on camera position
            world.update(camera.getPosition().x, camera.getPosition().z);

            // Update frustrum
            frustum.update(camera.getViewProjectionMatrix());
            
            // Draw all visible chunks
            shader.use();
            shader.setUniform("view", camera.getViewMatrix());
            shader.setUniform("projection", camera.getProjectionMatrix());
            shader.setUniform("showQuadEdges", showQuadEdges);

            Matrix4f model = new Matrix4f();
            for (Chunk chunk : world.getVisibleChunks()) {
                if (chunk.mesh != null) {
                    model.identity().translate(
                            chunk.pos.x * Chunk.SIZE_X,
                            0,
                            chunk.pos.z * Chunk.SIZE_Z
                    );
                    shader.setUniform("model", model);
                    chunk.mesh.draw();
                }
            }

            int chunksDrawn = 0;
            for (Chunk chunk : world.getVisibleChunks()) {
                if (chunk.mesh != null) {
                    float minX = chunk.pos.x * Chunk.SIZE_X;
                    float minZ = chunk.pos.z * Chunk.SIZE_Z;
                    if (frustum.isBoxVisible(minX, 0, minZ,
                            minX + Chunk.SIZE_X, Chunk.SIZE_Y, minZ + Chunk.SIZE_Z)) {
                        model.identity().translate(minX, 0, minZ);
                        shader.setUniform("model", model);
                        chunk.mesh.draw();
                        chunksDrawn++;
                    }
                }
            }

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events
            glfwPollEvents();
            
            // Update and render FPS to window title
            frameCount++;
            if (glfwGetTime() - fpsTimer >= 1.0) {
                glfwSetWindowTitle(window, "Voxel Engine — FPS: " + frameCount
                    + " | Chunks: " + chunksDrawn + "/" + world.chunks.size());
                frameCount = 0;
                fpsTimer = glfwGetTime();
            }
        }
    }

    public static void main(String[] args) {
        new VoxelEngine().run();
    }

}
