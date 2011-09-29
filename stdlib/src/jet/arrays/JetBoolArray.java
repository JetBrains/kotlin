package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetBoolArray extends JetArray<Boolean> {
    public  final boolean[] array;

    public JetBoolArray(boolean[] array) {
        this.array = array;
    }

    public JetBoolArray(int length) {
        this.array = new boolean[length];
    }

    @Override
    public Boolean get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Boolean value) {
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
