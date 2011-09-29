package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetLongArray extends JetArray<Long> {
    public final long[] array;

    public JetLongArray(long[] array) {
        this.array = array;
    }

    public JetLongArray(int length) {
        this.array = new long[length];
    }

    @Override
    public Long get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Long value) {
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
