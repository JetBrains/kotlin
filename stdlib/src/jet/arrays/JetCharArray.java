package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetCharArray extends JetArray<Character> {
    public  final char[] array;

    public JetCharArray(char[] array) {
        this.array = array;
    }

    public JetCharArray(int length) {
        this.array = new char[length];
    }

    @Override
    public Character get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Character value) {
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
