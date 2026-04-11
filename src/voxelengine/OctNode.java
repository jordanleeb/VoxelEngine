package voxelengine;

public class OctNode {
    public boolean isLeaf;
    public boolean solid;
    public OctNode[] children;
    
    public OctNode(boolean solid) {
        this.isLeaf = true;
        this.solid = solid;
        this.children = null;
    }
}
