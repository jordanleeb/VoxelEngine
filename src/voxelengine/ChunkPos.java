package voxelengine;

public class ChunkPos {
    public int x, z;
    
    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    @Override
    public int hashCode() {
        return 31 * x + z;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChunkPos)) return false;
        ChunkPos other = (ChunkPos) obj;
        return this.x == other.x && this.z == other.z;
    }
    
    @Override
    public String toString() {
        return "ChunkPos(" + this.x + ", " + this.z + ")";
    }
}
