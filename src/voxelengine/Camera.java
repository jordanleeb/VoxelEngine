package voxelengine;

import org.joml.Vector3f;
import org.joml.Matrix4f;

public class Camera {
    private Vector3f position;
    private float pitch;
    private float yaw;
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;
    
    public Camera() {
        this.position = new Vector3f(0.0f, 0.0f, 3.0f);
        this.pitch = 0.0f;
        this.yaw = -90.0f;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        
        projectionMatrix.perspective(
            (float) Math.toRadians(70), // field of view
            800f / 600f,                // aspect ratio
                                        // TODO: update on window resize
            0.01f,                      // near clipping plane
            1000f                       // far clipping plane
        );
    }
    
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    
    public Matrix4f getViewMatrix() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        Vector3f front = new Vector3f(
            (float) (Math.cos(yawRad) * Math.cos(pitchRad)),
            (float) Math.sin(pitchRad),
            (float) (Math.sin(yawRad) * Math.cos(pitchRad))
        );
        
        viewMatrix.identity().lookAt(
            position,
            new Vector3f(position).add(front),
            new Vector3f(0, 1, 0)
        );
        
        return viewMatrix;
    }
    
    public void move(float forwardAmount, float rightAmount, float upAmount) {
        float yawRad = (float) Math.toRadians(yaw);
        
        Vector3f front = new Vector3f(
            (float) Math.cos(yawRad),
            0.0f,
            (float) Math.sin(yawRad)
        ).normalize();
        
        Vector3f right = new Vector3f(front).cross(new Vector3f(0, 1, 0)).normalize();
        
        position.add(new Vector3f(front).mul(forwardAmount));
        position.add(new Vector3f(right).mul(rightAmount));
        position.y += upAmount;
    }
    
    public void rotate(float pitchDelta, float yawDelta) {
        yaw += yawDelta;
        pitch += pitchDelta;
        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
    }
}
