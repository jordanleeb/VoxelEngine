package voxelengine;

public class Chunk {

    public static final int SIZE_X = 32;
    public static final int SIZE_Y = 64;
    public static final int SIZE_Z = 32;

    public ChunkPos pos;
    public ChunkData data;
    public Mesh mesh;
    public boolean dirty;

    public Chunk(ChunkPos pos) {
        this.pos = pos;
        this.data = new ChunkData(SIZE_X, SIZE_Y, SIZE_Z);
        this.mesh = null;
        this.dirty = true;
    }
}
