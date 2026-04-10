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
    layout (location = 0) in vec3 aPos;    // receives attribute 0 (position)
    layout (location = 1) in vec3 aColor;  // receives attribute 1 (color)
    out vec3 vColor;                       // sends color to fragment shader
    void main() {
        vColor = aColor;                   // pass color through
        gl_Position = vec4(aPos, 1.0);     // set vertex position
    }""";
    private static final String FRAGMENT_SHADER_SOURCE = """
    #version 330 core
    in vec3 vColor;                        // receives color from vertex shader
    out vec4 FragColor;                    // the final pixel color
    void main() {
        FragColor = vec4(vColor, 1.0);     // use it as the pixel color
    }""";

    private long window;   // The window handle
    private Shader shader;
    private Mesh mesh;

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
        });

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        GL.createCapabilities();

        // Initialize shader
        this.shader = new Shader(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);

        // Initialize mesh
        float[] vertices = {
            // x,     y,    z,    r,    g,    b
            -0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, // top-left (red)
            0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, // top-right (green)
            0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, // bottom-right (blue)
            -0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 0.0f, // bottom-left (yellow)
        };

        int[] indices = {
            0, 1, 2, // first triangle
            2, 3, 0, // second triangle
        };

        this.mesh = new Mesh(vertices, indices);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Draw
            shader.use();
            mesh.draw();

            // Swap the color buffers
            glfwSwapBuffers(window);

            // Poll for window events
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new VoxelEngine().run();
    }

}
