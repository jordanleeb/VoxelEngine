package voxelengine;

public class IntBuffer {
    public int[] data;
    public int size;

    public IntBuffer(int capacity) {
        this.data = new int[capacity];
        this.size = 0;
    }

    public void add(int value) {
        if (size == data.length) {
            int[] newData = new int[data.length * 2];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
        data[size++] = value;
    }

    public int[] trimmed() {
        int[] result = new int[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }
}