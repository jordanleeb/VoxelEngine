package voxelengine;

import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private int vaoId;       // Handle to vertex array object
    private int vboId;       // Handle to vertex buffer object
    private int eboId;       // Handle to element buffer object
    private int vertexCount; // How many indices to draw
    
    public Mesh(float[] vertices, int[] indices) {
        this.vertexCount = indices.length;
        
        // Create and bind VAO
        this.vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create, bind and buffer vertex data into VBO
        this.vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Attribute 0: position — 7 floats * 4 bytes = 28 stride, starts at byte 0
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 28, 0);
        glEnableVertexAttribArray(0);

        // Attribute 1: color — 3 floats * 4 bytes = 12 offset
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 28, 12);
        glEnableVertexAttribArray(1);

        // Attribute 2: axis — 6 floats * 4 bytes = 24 offset
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 28, 24);
        glEnableVertexAttribArray(2);
        
        // Create, bind and buffer index data into EBO
        this.eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        
        // Unbind the VAO
        glBindVertexArray(0);
    }
    
    public void draw() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
        glDeleteVertexArrays(vaoId);
    }
}
