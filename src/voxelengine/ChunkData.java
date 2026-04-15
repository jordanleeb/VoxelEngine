package voxelengine;

public class ChunkData {

    public final int sizeX, sizeY, sizeZ;
    private final long[] data;
    private final int yStride, xStride;

    public ChunkData(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.yStride = sizeZ;
        this.xStride = sizeY * sizeZ;
        int totalBits = sizeX * sizeY * sizeZ;
        this.data = new long[(totalBits + 63) / 64];
    }

    private int bitIndex(int x, int y, int z) {
        return x * xStride + y * yStride + z;
    }

    public byte get(int x, int y, int z) {
        int idx = bitIndex(x, y, z);
        return (data[idx >> 6] & (1L << (idx & 63))) != 0 ? (byte) 1 : (byte) 0;
    }

    public void set(int x, int y, int z, byte value) {
        int idx = bitIndex(x, y, z);
        if (value != 0) {
            data[idx >> 6] |= (1L << (idx & 63));
        } else {
            data[idx >> 6] &= ~(1L << (idx & 63));
        }
    }
}
