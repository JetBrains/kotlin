package jet.arrays;

import jet.JetObject;
import jet.typeinfo.TypeInfo;

public abstract class JetArray<T> implements JetObject {
    public abstract T get(int index);
    public abstract void set(int index, T value);
    public abstract int getSize();

    public static JetArray newArray(int length, TypeInfo elementTypeInfo) {
        if(elementTypeInfo.equals(TypeInfo.BYTE_TYPE_INFO))
            return new JetByteArray(length);
        if(elementTypeInfo.equals(TypeInfo.SHORT_TYPE_INFO))
            return new JetShortArray(length);
        if(elementTypeInfo.equals(TypeInfo.INT_TYPE_INFO))
            return new JetIntArray(length);
        if(elementTypeInfo.equals(TypeInfo.LONG_TYPE_INFO))
            return new JetShortArray(length);
        if(elementTypeInfo.equals(TypeInfo.CHAR_TYPE_INFO))
            return new JetCharArray(length);
        if(elementTypeInfo.equals(TypeInfo.BOOL_TYPE_INFO))
            return new JetBoolArray(length);
        if(elementTypeInfo.equals(TypeInfo.FLOAT_TYPE_INFO))
            return new JetFloatArray(length);
        if(elementTypeInfo.equals(TypeInfo.DOUBLE_TYPE_INFO))
            return new JetDoubleArray(length);
        
        return new JetGenericArray(length, elementTypeInfo);
    }

    public static JetByteArray newByteArray(int length) {
        return new JetByteArray(length);
    }

    public static JetShortArray newShortArray(int length) {
        return new JetShortArray(length);
    }

    public static JetIntArray newIntArray(int length) {
        return new JetIntArray(length);
    }

    public static JetLongArray newLongArray(int length) {
        return new JetLongArray(length);
    }

    public static JetCharArray newCharArray(int length) {
        return new JetCharArray(length);
    }

    public static JetBoolArray newBoolArray(int length) {
        return new JetBoolArray(length);
    }

    public static JetFloatArray newFloatArray(int length) {
        return new JetFloatArray(length);
    }

    public static JetDoubleArray newDoubleArray(int length) {
        return new JetDoubleArray(length);
    }

    public static JetGenericArray newGenericArray(int length, TypeInfo typeInfo) {
        return new JetGenericArray(length, typeInfo);
    }
}
