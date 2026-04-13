package voxelengine;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class VoxelEngine {

    private static final String VERTEX_SHADER_SOURCE = """
    #version 330 core
    layout (location = 0) in vec3 aPos;    // vertex position
    layout (location = 1) in vec3 aColor;  // vertex color
    layout (location = 2) in float aAxis;  // face axis for grid lines
    uniform mat4 view;                     // camera view matrix
    uniform mat4 projection;               // perspective projection matrix
    out vec3 vColor;                       // passed to fragment shader
    out vec3 vPos;                         // world position for grid lines
    flat out float vAxis;                  // which axis this face is on
    void main() {
        vColor = aColor;
        vPos = aPos;
        vAxis = aAxis;
        gl_Position = projection * view * vec4(aPos, 1.0);
    }""";
    private static final String FRAGMENT_SHADER_SOURCE = """
    #version 330 core
    in vec3 vColor;                        // receives color from vertex shader
    in vec3 vPos;                          // world position for grid lines
    flat in float vAxis;                   // which axis this face is on
    uniform bool showQuadEdges;
    out vec4 FragColor;                    // the final pixel color
    void main() {
        if (showQuadEdges) {
            float near = 0.01;
            float far = 1000.0;
            float z = gl_FragCoord.z;
            float linearDepth = (2.0 * near) / (far + near - z * (far - near));
            float fade = clamp(1.0 - linearDepth * 55.0, 0.0, 1.0);
            vec3 nearColor = vec3(0.2, 1.0, 0.3);
            vec3 farColor = vec3(0.02, 0.05, 0.02);
            vec3 color = mix(farColor, nearColor, fade);
            FragColor = vec4(color, 1.0);
            return;
        }
        vec3 f = fract(vPos);
        float edge = 0.05;
        bool grid = false;
        if (vAxis != 0.0 && (f.x < edge || f.x > 1.0 - edge)) grid = true;
        if (vAxis != 1.0 && (f.y < edge || f.y > 1.0 - edge)) grid = true;
        if (vAxis != 2.0 && (f.z < edge || f.z > 1.0 - edge)) grid = true;
        if (grid) {
            FragColor = vec4(vColor * 0.3, 1.0);
        } else {
            FragColor = vec4(vColor, 1.0);
        }
    }""";

    private long window;   // The window handle
    private Camera camera;
    private Shader shader;
    private Mesh mesh;

    private double lastFrameTime;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    
    private boolean showQuadEdges = false;
    private boolean mouseCaptured = true;

    public void run() {
        init();
        loop();

        // Cleanup mesh and shader
        mesh.cleanup();
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

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Initialize shader
        this.shader = new Shader(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
        
        // Initialize camera
        this.camera = new Camera();

        // Create test octree
        Octree tree = new Octree(64);
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                double nx = x * 0.05;
                double nz = z * 0.05;
                double height = 0;
                height += PerlinNoise.noise(nx, 0, nz) * 20;
                height += PerlinNoise.noise(nx * 2, 0, nz * 2) * 10;
                height += PerlinNoise.noise(nx * 4, 0, nz * 4) * 5;
                height += 32;

                for (int y = 0; y < (int) height && y < 64; y++) {
                    tree.set(x, y, z, true);
                }
            }
        }
        
        // Build test mesh from octree
        this.mesh = ChunkMesher.buildMesh(tree);
        
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
            
            // Draw
            shader.use();
            
            shader.setUniform("view", camera.getViewMatrix());
            shader.setUniform("projection", camera.getProjectionMatrix());
            shader.setUniform("showQuadEdges", showQuadEdges);
            
            mesh.draw();

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events
            glfwPollEvents();
            
            // Update and render FPS to window title
            frameCount++;
            if (glfwGetTime() - fpsTimer >= 1.0) {
                glfwSetWindowTitle(window, "Voxel Engine — FPS: " + frameCount);
                frameCount = 0;
                fpsTimer = glfwGetTime();
            }
        }
    }

    public static void main(String[] args) {
        new VoxelEngine().run();
    }

}
