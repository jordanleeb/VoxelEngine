package voxelengine;

public class FloatBuffer {
    public float[] data;
    public int size;
    
    public FloatBuffer(int capacity) {
        this.data = new float[capacity];
        this.size = 0;
    }
    
    public void add(float value) {
        if (size == data.length) {
            float[] newData = new float[data.length * 2];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
        }
        data[size++] = value;
    }
    
    public float[] trimmed() {
        float[] result = new float[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }
}
