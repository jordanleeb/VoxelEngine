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

        float minRow = r.top;
        float maxRow = r.bottom + 1;
        float minCol = r.left;
        float maxCol = r.right + 1;

        int base = vertices.size / 4;

        if (axis == 0) {
            addVertex(vertices, pos, minRow, minCol, axis);
            addVertex(vertices, pos, minRow, maxCol, axis);
            addVertex(vertices, pos, maxRow, maxCol, axis);
            addVertex(vertices, pos, maxRow, minCol, axis);
        } else if (axis == 1) {
            addVertex(vertices, minRow, pos, minCol, axis);
            addVertex(vertices, minRow, pos, maxCol, axis);
            addVertex(vertices, maxRow, pos, maxCol, axis);
            addVertex(vertices, maxRow, pos, minCol, axis);
        } else {
            addVertex(vertices, minRow, minCol, pos, axis);
            addVertex(vertices, minRow, maxCol, pos, axis);
            addVertex(vertices, maxRow, maxCol, pos, axis);
            addVertex(vertices, maxRow, minCol, pos, axis);
        }

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

    private static void addVertex(FloatBuffer vertices, float x, float y, float z, int axis) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add((float) axis);
    }
}
