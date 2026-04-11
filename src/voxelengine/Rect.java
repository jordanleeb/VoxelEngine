package voxelengine;

public class Rect {
    public int top;
    public int left;
    public int bottom;
    public int right;
    public int id;
    
    public Rect(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }
    
    public int getWidth() { return right - left + 1; }
    
    @Override
    public String toString() {
        return "Rect(" + top + ", " + left + ", " + bottom + ", " + right + ")";
    }
}
