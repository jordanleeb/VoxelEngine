package voxelengine;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {
    public static Mesh buildMesh(Octree tree) {
        FloatBuffer vertices = new FloatBuffer(1024);
        IntBuffer indices = new IntBuffer(1024);
        
        Dissector dissector = new Dissector();

        for (int axis = 0; axis < 3; axis++) {
            for (int layer = 0; layer < tree.size; layer++) {
                boolean[][][] slices = SliceExtractor.extract(tree, axis, layer);

                // Positive face
                List<Rect> posRects = dissector.solve(slices[0]);
                for (Rect r : posRects) {
                    addQuad(vertices, indices, r, axis, layer, true);
                }

                // Negative face
                List<Rect> negRects = dissector.solve(slices[1]);
                for (Rect r : negRects) {
                    addQuad(vertices, indices, r, axis, layer, false);
                }
            }
        }

        return new Mesh(vertices.trimmed(), indices.trimmed());
    }
    
    private static void addQuad(FloatBuffer vertices, IntBuffer indices,
                                Rect r, int axis, int layer, boolean positive) {
        float pos = positive ? layer + 1 : layer;

        // The 4 corners as float arrays [x, y, z]
        float[][] corners = new float[4][3];

        float minRow = r.top;
        float maxRow = r.bottom + 1;
        float minCol = r.left;
        float maxCol = r.right + 1;

        for (int i = 0; i < 4; i++) {
            float rowVal = (i < 2) ? minRow : maxRow;
            float colVal = (i == 0 || i == 3) ? minCol : maxCol;

            if (axis == 0) {
                corners[i][0] = pos;
                corners[i][1] = rowVal;
                corners[i][2] = colVal;
            } else if (axis == 1) {
                corners[i][0] = rowVal;
                corners[i][1] = pos;
                corners[i][2] = colVal;
            } else {
                corners[i][0] = rowVal;
                corners[i][1] = colVal;
                corners[i][2] = pos;
            }
        }

        // Color — flat green for now
        float cr = 0.3f, cg = 0.8f, cb = 0.3f;

        // Base index for this quad
        int base = vertices.size / 7;

        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            vertices.add(corners[i][0]);
            vertices.add(corners[i][1]);
            vertices.add(corners[i][2]);
            vertices.add(cr);
            vertices.add(cg);
            vertices.add(cb);
            vertices.add((float) axis);
        }

        // Add 6 indices (two triangles)
        boolean flip = (axis == 1) != positive;
        if (flip) {
            indices.add(base);
            indices.add(base + 3);
            indices.add(base + 2);
            indices.add(base + 2);
            indices.add(base + 1);
            indices.add(base);
        } else {
            indices.add(base);
            indices.add(base + 1);
            indices.add(base + 2);
            indices.add(base + 2);
            indices.add(base + 3);
            indices.add(base);
        }
    }
}
