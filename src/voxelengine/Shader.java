package voxelengine;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    // Handle to the compiled and linked shader program on the GPU
    private int programId;
    
    public Shader(String vertexSource, String fragmentSource) {
        // Compile vertex shader
        int vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderId, vertexSource);
        glCompileShader(vertexShaderId);
        
        if (glGetShaderi(vertexShaderId, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Vertex shader compilation failed:\n" + glGetShaderInfoLog(vertexShaderId));
        
        // Compile fragment shader
        int fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderId, fragmentSource);
        glCompileShader(fragmentShaderId);
        
        if (glGetShaderi(fragmentShaderId, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Fragment shader compilation failed:\n" + glGetShaderInfoLog(fragmentShaderId));
        
        // Link shaders into program
        this.programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        
        if(glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Program linking failed:\n" + glGetProgramInfoLog(programId));
        
        // Clean up, deleting individual shaders
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }
    
    public void use() {
        glUseProgram(programId);
    }
    
    public void cleanup() {
        glDeleteProgram(programId);
    }
}
