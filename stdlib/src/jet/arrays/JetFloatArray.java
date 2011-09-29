package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetFloatArray extends JetArray<Float> {
    public  final float[] array;

    public JetFloatArray(float[] array) {
        this.array = array;
    }

    public JetFloatArray(int length) {
        this.array = new float[length];
    }

    @Override
    public Float get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Float value) {
        array[index] = value;
    }

    @Override
    public int getSize() {
        return array.length;
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return null;
    }
}
