package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetGenericArray<T> extends JetArray<T> {
    public  final T[] array;
    private final TypeInfo<T> typeInfo;

    public JetGenericArray(T[] array, TypeInfo<T> typeInfo) {
        this.array = array;
        this.typeInfo = typeInfo;
    }

    public JetGenericArray(int length, TypeInfo<T> typeInfo) {
        this.array = (T[]) TypeInfo.newArray(length, typeInfo);
        this.typeInfo = typeInfo;
    }

    @Override
    public T get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, T value) {
        array[index] = value;
    }

    @Override
    public int getSize() {
        return array.length;
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }
}
