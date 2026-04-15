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
                if (layer + 1 < size) {
                    boolean posNeighbor = false;
                    if (axis == 0) {
                        posNeighbor = tree.get(x + 1, y, z);
                    } else if (axis == 1) {
                        posNeighbor = tree.get(x, y + 1, z);
                    } else {
                        posNeighbor = tree.get(x, y, z + 1);
                    }
                    if (!posNeighbor) {
                        posSlice[row][col] = true;
                    }
                }
                
                // Negative face: solid here, air behind
                if (layer - 1 >= 0) {
                    boolean negNeighbor = false;
                    if (axis == 0) {
                        negNeighbor = tree.get(x - 1, y, z);
                    } else if (axis == 1) {
                        negNeighbor = tree.get(x, y - 1, z);
                    } else {
                        negNeighbor = tree.get(x, y, z - 1);
                    }
                    if (!negNeighbor) {
                        negSlice[row][col] = true;
                    }
                }
            }
        }

        return new boolean[][][]{posSlice, negSlice};
    }
    
    public static boolean[][][] extract(ChunkData data, int axis, int layer) {
        int rows, cols, layers;
        if (axis == 0) {
            layers = data.sizeX;
            rows = data.sizeY;
            cols = data.sizeZ;
        } else if (axis == 1) {
            layers = data.sizeY;
            rows = data.sizeX;
            cols = data.sizeZ;
        } else {
            layers = data.sizeZ;
            rows = data.sizeX;
            cols = data.sizeY;
        }
        boolean[][] posSlice = new boolean[rows][cols];
        boolean[][] negSlice = new boolean[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
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

                boolean solid = data.get(x,y,z) != 0;
                if (!solid) {
                    continue;
                }

                // Positive face: solid here, air ahead
                if (layer + 1 < layers) {
                    boolean posNeighbor = false;
                    if (axis == 0) {
                        posNeighbor = data.get(x + 1, y, z) != 0;
                    } else if (axis == 1) {
                        posNeighbor = data.get(x, y + 1, z) != 0;
                    } else {
                        posNeighbor = data.get(x, y, z + 1) != 0;
                    }
                    if (!posNeighbor) {
                        posSlice[row][col] = true;
                    }
                }

                // Negative face: solid here, air behind
                if (layer - 1 >= 0) {
                    boolean negNeighbor = false;
                    if (axis == 0) {
                        negNeighbor = data.get(x - 1, y, z) != 0;
                    } else if (axis == 1) {
                        negNeighbor = data.get(x, y - 1, z) != 0;
                    } else {
                        negNeighbor = data.get(x, y, z - 1) != 0;
                    }
                    if (!negNeighbor) {
                        negSlice[row][col] = true;
                    }
                }
            }
        }

        return new boolean[][][]{posSlice, negSlice};
    }
    
    public static boolean[][][] extract(ChunkData data, int axis, int layer,
                                        World world, ChunkPos chunkPos) {
        int rows, cols, layers;
        if (axis == 0) {
            layers = data.sizeX;
            rows = data.sizeY;
            cols = data.sizeZ;
        } else if (axis == 1) {
            layers = data.sizeY;
            rows = data.sizeX;
            cols = data.sizeZ;
        } else {
            layers = data.sizeZ;
            rows = data.sizeX;
            cols = data.sizeY;
        }
        boolean[][] posSlice = new boolean[rows][cols];
        boolean[][] negSlice = new boolean[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
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

                boolean solid = data.get(x, y, z) != 0;
                if (!solid) {
                    continue;
                }

                // World coordinates for boundary lookups
                int wx = chunkPos.x * Chunk.SIZE_X + x;
                int wy = y;
                int wz = chunkPos.z * Chunk.SIZE_Z + z;

                // Positive face
                if (layer + 1 < layers) {
                    boolean posNeighbor = false;
                    if (axis == 0) {
                        posNeighbor = data.get(x + 1, y, z) != 0;
                    } else if (axis == 1) {
                        posNeighbor = data.get(x, y + 1, z) != 0;
                    } else {
                        posNeighbor = data.get(x, y, z + 1) != 0;
                    }
                    if (!posNeighbor) {
                        posSlice[row][col] = true;
                    }
                } else {
                    int nwx = wx, nwy = wy, nwz = wz;
                    if (axis == 0) {
                        nwx++;
                    } else if (axis == 1) {
                        nwy++;
                    } else {
                        nwz++;
                    }
                    if (world.getBlock(nwx, nwy, nwz) == 0) {
                        posSlice[row][col] = true;
                    }
                }

                // Negative face
                if (layer - 1 >= 0) {
                    boolean negNeighbor = false;
                    if (axis == 0) {
                        negNeighbor = data.get(x - 1, y, z) != 0;
                    } else if (axis == 1) {
                        negNeighbor = data.get(x, y - 1, z) != 0;
                    } else {
                        negNeighbor = data.get(x, y, z - 1) != 0;
                    }
                    if (!negNeighbor) {
                        negSlice[row][col] = true;
                    }
                } else {
                    int nwx = wx, nwy = wy, nwz = wz;
                    if (axis == 0) {
                        nwx--;
                    } else if (axis == 1) {
                        nwy--;
                    } else {
                        nwz--;
                    }
                    if (world.getBlock(nwx, nwy, nwz) == 0) {
                        negSlice[row][col] = true;
                    }
                }
            }
        }

        return new boolean[][][]{posSlice, negSlice};
    }
}
