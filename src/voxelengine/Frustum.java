package voxelengine;

import org.joml.Matrix4f;

public class Frustum {
    private final float[][] planes = new float[6][4]; // 6 planes, each [a, b, c, d]

    public void update(Matrix4f vp) {
        // Left
        planes[0][0] = vp.m03() + vp.m00();
        planes[0][1] = vp.m13() + vp.m10();
        planes[0][2] = vp.m23() + vp.m20();
        planes[0][3] = vp.m33() + vp.m30();
        // Right
        planes[1][0] = vp.m03() - vp.m00();
        planes[1][1] = vp.m13() - vp.m10();
        planes[1][2] = vp.m23() - vp.m20();
        planes[1][3] = vp.m33() - vp.m30();
        // Bottom
        planes[2][0] = vp.m03() + vp.m01();
        planes[2][1] = vp.m13() + vp.m11();
        planes[2][2] = vp.m23() + vp.m21();
        planes[2][3] = vp.m33() + vp.m31();
        // Top
        planes[3][0] = vp.m03() - vp.m01();
        planes[3][1] = vp.m13() - vp.m11();
        planes[3][2] = vp.m23() - vp.m21();
        planes[3][3] = vp.m33() - vp.m31();
        // Near
        planes[4][0] = vp.m03() + vp.m02();
        planes[4][1] = vp.m13() + vp.m12();
        planes[4][2] = vp.m23() + vp.m22();
        planes[4][3] = vp.m33() + vp.m32();
        // Far
        planes[5][0] = vp.m03() - vp.m02();
        planes[5][1] = vp.m13() - vp.m12();
        planes[5][2] = vp.m23() - vp.m22();
        planes[5][3] = vp.m33() - vp.m32();

        // Normalize all planes
        for (int i = 0; i < 6; i++) {
            float len = (float) Math.sqrt(
                planes[i][0] * planes[i][0] +
                planes[i][1] * planes[i][1] +
                planes[i][2] * planes[i][2]);
            planes[i][0] /= len;
            planes[i][1] /= len;
            planes[i][2] /= len;
            planes[i][3] /= len;
        }
    }

    public boolean isBoxVisible(float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            float a = planes[i][0], b = planes[i][1];
            float c = planes[i][2], d = planes[i][3];

            // Test the positive vertex (corner closest to plane normal)
            float px = a > 0 ? maxX : minX;
            float py = b > 0 ? maxY : minY;
            float pz = c > 0 ? maxZ : minZ;

            if (a * px + b * py + c * pz + d < 0) {
                return false; // Entirely outside this plane
            }
        }
        return true;
    }
}