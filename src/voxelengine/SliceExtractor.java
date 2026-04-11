package voxelengine;

public class SliceExtractor {
    public static boolean[][] extract(Octree tree, int axis, int layer) {
        int size = tree.size;
        boolean[][] slice = new boolean[size][size];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                // Map 2D (row, col) to 3D coordinates based on axis
                int x, y, z;
                if (axis == 0) {       // X-facing
                    x = layer; y = row; z = col;
                } else if (axis == 1) { // Y-facing
                    x = row; y = layer; z = col;
                } else {               // Z-facing
                    x = row; y = col; z = layer;
                }

                boolean solid = tree.get(x, y, z);

                // Check neighbor in the positive axis direction
                boolean neighborSolid = false;
                if (axis == 0 && layer + 1 < size) neighborSolid = tree.get(x + 1, y, z);
                if (axis == 1 && layer + 1 < size) neighborSolid = tree.get(x, y + 1, z);
                if (axis == 2 && layer + 1 < size) neighborSolid = tree.get(x, y, z + 1);

                // Face is visible where solid meets air
                slice[row][col] = solid && !neighborSolid;
            }
        }

        return slice;
    }
}
