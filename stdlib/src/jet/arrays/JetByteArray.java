package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetByteArray extends JetArray<Byte> {
    public  final byte[] array;

    public JetByteArray(byte[] array) {
        this.array = array;
    }

    public JetByteArray(int length) {
        this.array = new byte[length];
    }

    @Override
    public Byte get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Byte value) {
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
