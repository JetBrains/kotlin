package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetIntArray extends JetArray<Integer> {
    public  final int[] array;

    public JetIntArray(int[] array) {
        this.array = array;
    }

    public JetIntArray(int length) {
        this.array = new int[length];
    }

    @Override
    public Integer get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Integer value) {
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
