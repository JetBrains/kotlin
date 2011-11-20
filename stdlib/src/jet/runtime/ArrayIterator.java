package jet.runtime;

import jet.*;
import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

/**
 * @author alex.tkachman
 */
public abstract class ArrayIterator<T> implements Iterator<T>, JetObject {
    private final int size;
    protected int index;

    protected ArrayIterator(int size) {
        this.size = size;
    }

    @Override
    public boolean getHasNext() {
        return index < size;
    }

    private static class GenericIterator<T> extends ArrayIterator<T> {
        private final T[] array;
        private final TypeInfo elementTypeInfo;

        private GenericIterator(T[] array, TypeInfo elementTypeInfo) {
            super(array.length);
            this.array = array;
            this.elementTypeInfo = elementTypeInfo;
        }

        @Override
        public T next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return TypeInfo.getTypeInfo(GenericIterator.class, false, null, new TypeInfoProjection[] {(TypeInfoProjection) elementTypeInfo});
        }
    }
    
    public static <T> Iterator<T> iterator(T[] array, TypeInfo elementTypeInfo) {
        return new GenericIterator<T>(array, elementTypeInfo);
    }

    private static class ArrayByteIterator extends ByteIterator {
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayByteIterator.class, false);

        private final byte[] array;
        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayByteIterator(byte[] array) {
            this.array = array;
        }

        @Override
        public byte nextByte() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static ByteIterator iterator(byte[] array) {
        return new ArrayByteIterator(array);
    }

    private static class ArrayShortIterator extends ShortIterator {
        private final short[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayShortIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayShortIterator(short[] array) {
            this.array = array;
        }

        @Override
        public short nextShort() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static ShortIterator iterator(short[] array) {
        return new ArrayShortIterator(array);
    }

    private static class ArrayIntegerIterator extends IntIterator {
        private final int[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayIntegerIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayIntegerIterator(int[] array) {
            this.array = array;
        }

        @Override
        public int nextInt() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static IntIterator iterator(int[] array) {
        return new ArrayIntegerIterator(array);
    }

    private static class ArrayLongIterator extends LongIterator {
        private final long[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayLongIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayLongIterator(long[] array) {
            this.array = array;
        }

        @Override
        public long nextLong() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static LongIterator iterator(long[] array) {
        return new ArrayLongIterator(array);
    }

    private static class ArrayFloatIterator extends FloatIterator {
        private final float[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayFloatIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayFloatIterator(float[] array) {
            this.array = array;
        }

        @Override
        public float nextFloat() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static FloatIterator iterator(float[] array) {
        return new ArrayFloatIterator(array);
    }

    private static class ArrayDoubleIterator extends DoubleIterator {
        private final double[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayDoubleIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayDoubleIterator(double[] array) {
            this.array = array;
        }

        @Override
        public double nextDouble() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static DoubleIterator iterator(double[] array) {
        return new ArrayDoubleIterator(array);
    }

    private static class ArrayCharacterIterator extends CharIterator {
        private final char[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayCharacterIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayCharacterIterator(char[] array) {
            this.array = array;
        }

        @Override
        public char nextChar() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static CharIterator iterator(char[] array) {
        return new ArrayCharacterIterator(array);
    }

    private static class ArrayBooleanIterator extends BooleanIterator {
        private final boolean[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ArrayBooleanIterator.class, false);

        private int index;

        @Override
        public boolean getHasNext() {
            return index < array.length;
        }

        private ArrayBooleanIterator(boolean[] array) {
            this.array = array;
        }

        @Override
        public boolean nextBoolean() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static BooleanIterator iterator(boolean[] array) {
        return new ArrayBooleanIterator(array);
    }
}
