package jet.arrays;

import jet.Iterator;
import jet.JetObject;
import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

import java.lang.reflect.GenericArrayType;
import java.util.Arrays;

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
    public boolean hasNext() {
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

    private static class ByteIterator extends ArrayIterator<Byte> {
        private final byte[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ByteIterator.class, false);

        private ByteIterator(byte[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Byte next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Byte> iterator(byte[] array) {
        return new ByteIterator(array);
    }

    private static class ShortIterator extends ArrayIterator<Short> {
        private final short[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ShortIterator.class, false);

        private ShortIterator(short[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Short next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Short> iterator(short[] array) {
        return new ShortIterator(array);
    }

    private static class IntegerIterator extends ArrayIterator<Integer> {
        private final int[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(IntegerIterator.class, false);

        private IntegerIterator(int[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Integer next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Integer> iterator(int[] array) {
        return new IntegerIterator(array);
    }

    private static class LongIterator extends ArrayIterator<Long> {
        private final long[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(LongIterator.class, false);

        private LongIterator(long[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Long next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Long> iterator(long[] array) {
        return new LongIterator(array);
    }

    private static class FloatIterator extends ArrayIterator<Float> {
        private final float[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(FloatIterator.class, false);

        private FloatIterator(float[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Float next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Float> iterator(float[] array) {
        return new FloatIterator(array);
    }

    private static class DoubleIterator extends ArrayIterator<Double> {
        private final double[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(DoubleIterator.class, false);

        private DoubleIterator(double[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Double next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Double> iterator(double[] array) {
        return new DoubleIterator(array);
    }

    private static class CharacterIterator extends ArrayIterator<Character> {
        private final char[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(CharacterIterator.class, false);

        private CharacterIterator(char[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Character next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Character> iterator(char[] array) {
        return new CharacterIterator(array);
    }

    private static class BooleanIterator extends ArrayIterator<Boolean> {
        private final boolean[] array;
        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(BooleanIterator.class, false);

        private BooleanIterator(boolean[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public Boolean next() {
            return array[index++];
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }

    public static Iterator<Boolean> iterator(boolean[] array) {
        return new BooleanIterator(array);
    }

    public static void main(String[] args) {
        String[] strings = {"Byte", "byte", "Short", "short", "Integer", "int", "Long", "long", "Float", "float", "Double", "double", "Character", "char", "Boolean", "boolean"};
        for(int i = 0; i != strings.length; i += 2) {
            String boxed = strings[i];
            String unboxed = strings[i+1];
            System.out.println("    private static class " + boxed + "Iterator extends ArrayIterator<" + boxed + "> {\n" +
                               "        private final " + unboxed + "[] array;\n" +
                               "        private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(" + boxed + "Iterator.class, false);\n" +
                               "\n" +
                               "        private " + boxed + "Iterator(" + unboxed + "[] array) {\n" +
                               "            super(array.length);\n" +
                               "            this.array = array;\n" +
                               "        }\n" +
                               "\n" +
                               "        @Override\n" +
                               "        public " + boxed + " next() {\n" +
                               "            return array[index++];\n" +
                               "        }\n" +
                               "\n" +
                               "        @Override\n" +
                               "        public TypeInfo<?> getTypeInfo() {\n" +
                               "            return typeInfo;\n" +
                               "        }\n" +
                               "    }\n" +
                               "\n" +
                               "    public static Iterator<" + boxed + "> iterator(" + unboxed + "[] array) {\n" +
                               "        return new " +  boxed + "Iterator(array);\n" +
                               "    }\n" +
                               "");
        }
    }
}
