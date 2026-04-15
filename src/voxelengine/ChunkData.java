package voxelengine;

public class ChunkData {
    private byte[][][] data;
    public final int sizeX, sizeY, sizeZ;

    public ChunkData(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.data = new byte[sizeX][sizeY][sizeZ];
    }

    public byte get(int x, int y, int z) { return data[x][y][z]; }
    public void set(int x, int y, int z, byte value) { data[x][y][z] = value; }
}
