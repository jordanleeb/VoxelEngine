package voxelengine;

public class SliceExtractor {
    public static boolean[][][] extract(Octree tree, int axis, int layer) {
        int size = tree.size;
        boolean[][] posSlice = new boolean[size][size];
        boolean[][] negSlice = new boolean[size][size];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x, y, z;
                if (axis == 0) {
                    x = layer;
                    y = row;
                    z = col;
                } else if (axis == 1) {
                    x = row;
                    y = layer;
                    z = col;
                } else {
                    x = row;
                    y = col;
                    z = layer;
                }

                boolean solid = tree.get(x, y, z);
                if (!solid) {
                    continue;
                }

                // Positive face: solid here, air ahead
                boolean posNeighbor = false;
                if (layer + 1 < size) {
                    if (axis == 0) {
                        posNeighbor = tree.get(x + 1, y, z);
                    } else if (axis == 1) {
                        posNeighbor = tree.get(x, y + 1, z);
                    } else {
                        posNeighbor = tree.get(x, y, z + 1);
                    }
                }
                if (!posNeighbor) {
                    posSlice[row][col] = true;
                }

                // Negative face: solid here, air behind
                boolean negNeighbor = false;
                if (layer - 1 >= 0) {
                    if (axis == 0) {
                        negNeighbor = tree.get(x - 1, y, z);
                    } else if (axis == 1) {
                        negNeighbor = tree.get(x, y - 1, z);
                    } else {
                        negNeighbor = tree.get(x, y, z - 1);
                    }
                }
                if (!negNeighbor) {
                    negSlice[row][col] = true;
                }
            }
        }

        return new boolean[][][]{posSlice, negSlice};
    }
}
