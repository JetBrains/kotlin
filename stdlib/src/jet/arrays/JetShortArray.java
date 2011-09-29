package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetShortArray extends JetArray<Short> {
    public final short[] array;

    public JetShortArray(short[] array) {
        this.array = array;
    }

    public JetShortArray(int length) {
        this.array = new short[length];
    }

    @Override
    public Short get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Short value) {
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
