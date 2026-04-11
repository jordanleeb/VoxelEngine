package voxelengine;

public class Octree {
    public OctNode root;
    public int size;
    
    public Octree(int size) {
        this.root = new OctNode(false);
        this.size = size;
    }
    
    public boolean get(int x, int y, int z) {
        return get(root, x, y, z, size);
    }
    
    private boolean get(OctNode node, int x, int y, int z, int size) {
        if (node.isLeaf) return node.solid;

        int half = size / 2;
        int childIndex = 0;
        if (x >= half) childIndex |= 4;
        if (y >= half) childIndex |= 2;
        if (z >= half) childIndex |= 1;

        return get(node.children[childIndex], x % half, y % half, z % half, half);
    }
    
    public void set(int x, int y, int z, boolean value) {
        root = set(root, x, y, z, size, value);
    }
    
    private OctNode set(OctNode node, int x, int y, int z, int size, boolean value) {
        // Base case: single voxel
        if (size == 1) {
            return new OctNode(value);
        }

        // If this is a leaf, we need to split it into 8 children
        // before we can change just one voxel
        if (node.isLeaf) {
            if (node.solid == value) return node; // already the right value
            // Split: make 8 children all with the old value
            node.isLeaf = false;
            node.children = new OctNode[8];
            for (int i = 0; i < 8; i++) {
                node.children[i] = new OctNode(node.solid);
            }
        }

        // Recurse into the correct child
        int half = size / 2;
        int childIndex = 0;
        if (x >= half) {
            childIndex |= 4;
        }
        if (y >= half) {
            childIndex |= 2;
        }
        if (z >= half) {
            childIndex |= 1;
        }

        node.children[childIndex] = set(node.children[childIndex], x % half, y % half, z % half, half, value);

        // Try to collapse: if all 8 children are leaves with the same value
        boolean allSame = true;
        boolean firstVal = node.children[0].isLeaf ? node.children[0].solid : false;
        if (!node.children[0].isLeaf) {
            allSame = false;
        }
        for (int i = 1; i < 8 && allSame; i++) {
            if (!node.children[i].isLeaf || node.children[i].solid != firstVal) {
                allSame = false;
            }
        }
        if (allSame) {
            return new OctNode(firstVal);
        }

        return node;
    }
}
