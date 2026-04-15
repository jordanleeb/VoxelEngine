package voxelengine;

import java.util.ArrayList;
import java.util.List;

public class ChunkMesher {
    public static Mesh buildMesh(Octree tree) {
        int naiveQuads = 0;
        int dissectedQuads = 0;
        
        FloatBuffer vertices = new FloatBuffer(1024);
        IntBuffer indices = new IntBuffer(1024);
        
        Dissector dissector = new Dissector();

        for (int axis = 0; axis < 3; axis++) {
            for (int layer = 0; layer < tree.size; layer++) {
                boolean[][][] slices = SliceExtractor.extract(tree, axis, layer);
                
                // Count naive quads (one per visible face)
                for (int r = 0; r < slices[0].length; r++) {
                    for (int c = 0; c < slices[0][r].length; c++) {
                        if (slices[0][r][c]) {
                            naiveQuads++;
                        }
                    }
                }

                for (int r = 0; r < slices[1].length; r++)
                    for (int c = 0; c < slices[1][r].length; c++)
                        if (slices[1][r][c])
                            naiveQuads++;

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
                
                // Count dissected quads
                dissectedQuads += posRects.size() + negRects.size();
            }
        }
        
        System.out.println("Naive quads: " + naiveQuads);
        System.out.println("Dissected quads: " + dissectedQuads);
        System.out.println("Saved: " + (naiveQuads - dissectedQuads) + " ("
                + String.format("%.1f", (1.0 - (double) dissectedQuads / naiveQuads) * 100) + "%)");

        return new Mesh(vertices.trimmed(), indices.trimmed());
    }
    
    public static Mesh buildMesh(ChunkData data) {
        FloatBuffer vertices = new FloatBuffer(1024);
        IntBuffer indices = new IntBuffer(1024);
        Dissector dissector = new Dissector();

        for (int axis = 0; axis < 3; axis++) {
            int layers;
            if (axis == 0) {
                layers = data.sizeX;
            } else if (axis == 1) {
                layers = data.sizeY;
            } else {
                layers = data.sizeZ;
            }

            for (int layer = 0; layer < layers; layer++) {
                boolean[][][] slices = SliceExtractor.extract(data, axis, layer);

                List<Rect> posRects = dissector.solve(slices[0]);
                for (Rect r : posRects) {
                    addQuad(vertices, indices, r, axis, layer, true);
                }

                List<Rect> negRects = dissector.solve(slices[1]);
                for (Rect r : negRects) {
                    addQuad(vertices, indices, r, axis, layer, false);
                }
            }
        }

        return new Mesh(vertices.trimmed(), indices.trimmed());
    }
    
    public static Mesh buildMesh(ChunkData data, World world, ChunkPos pos) {
        FloatBuffer vertices = new FloatBuffer(1024);
        IntBuffer indices = new IntBuffer(1024);
        Dissector dissector = new Dissector();

        for (int axis = 0; axis < 3; axis++) {
            int layers;
            if (axis == 0) {
                layers = data.sizeX;
            } else if (axis == 1) {
                layers = data.sizeY;
            } else {
                layers = data.sizeZ;
            }

            for (int layer = 0; layer < layers; layer++) {
                boolean[][][] slices = SliceExtractor.extract(data, axis, layer, world, pos);

                List<Rect> posRects = dissector.solve(slices[0]);
                for (Rect r : posRects) {
                    addQuad(vertices, indices, r, axis, layer, true);
                }

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

        // Base index for this quad
        int base = vertices.size / 4;

        // Add 4 vertices
        for (int i = 0; i < 4; i++) {
            vertices.add(corners[i][0]);
            vertices.add(corners[i][1]);
            vertices.add(corners[i][2]);
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
