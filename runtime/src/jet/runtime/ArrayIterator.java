/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jet.runtime;

import jet.*;

import java.util.Iterator;

public abstract class ArrayIterator<T> implements Iterator<T> {
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

        private GenericIterator(T[] array) {
            super(array.length);
            this.array = array;
        }

        @Override
        public T next() {
            return array[index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Mutating method called on a Kotlin Iterator");
        }
    }
    
    public static <T> Iterator<T> iterator(T[] array) {
        return new GenericIterator<T>(array);
    }

    private static class ArrayByteIterator extends ByteIterator {
        private final byte[] array;
        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayByteIterator(byte[] array) {
            this.array = array;
        }

        @Override
        public byte nextByte() {
            return array[index++];
        }
    }

    public static ByteIterator iterator(byte[] array) {
        return new ArrayByteIterator(array);
    }

    private static class ArrayShortIterator extends ShortIterator {
        private final short[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayShortIterator(short[] array) {
            this.array = array;
        }

        @Override
        public short nextShort() {
            return array[index++];
        }
    }

    public static ShortIterator iterator(short[] array) {
        return new ArrayShortIterator(array);
    }

    private static class ArrayIntIterator extends IntIterator {
        private final int[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayIntIterator(int[] array) {
            this.array = array;
        }

        @Override
        public int nextInt() {
            return array[index++];
        }
    }

    public static IntIterator iterator(int[] array) {
        return new ArrayIntIterator(array);
    }

    private static class ArrayLongIterator extends LongIterator {
        private final long[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayLongIterator(long[] array) {
            this.array = array;
        }

        @Override
        public long nextLong() {
            return array[index++];
        }
    }

    public static LongIterator iterator(long[] array) {
        return new ArrayLongIterator(array);
    }

    private static class ArrayFloatIterator extends FloatIterator {
        private final float[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayFloatIterator(float[] array) {
            this.array = array;
        }

        @Override
        public float nextFloat() {
            return array[index++];
        }
    }

    public static FloatIterator iterator(float[] array) {
        return new ArrayFloatIterator(array);
    }

    private static class ArrayDoubleIterator extends DoubleIterator {
        private final double[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayDoubleIterator(double[] array) {
            this.array = array;
        }

        @Override
        public double nextDouble() {
            return array[index++];
        }
    }

    public static DoubleIterator iterator(double[] array) {
        return new ArrayDoubleIterator(array);
    }

    private static class ArrayCharacterIterator extends CharIterator {
        private final char[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayCharacterIterator(char[] array) {
            this.array = array;
        }

        @Override
        public char nextChar() {
            return array[index++];
        }
    }

    public static CharIterator iterator(char[] array) {
        return new ArrayCharacterIterator(array);
    }

    private static class ArrayBooleanIterator extends BooleanIterator {
        private final boolean[] array;

        private int index;

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        private ArrayBooleanIterator(boolean[] array) {
            this.array = array;
        }

        @Override
        public boolean nextBoolean() {
            return array[index++];
        }
    }

    public static BooleanIterator iterator(boolean[] array) {
        return new ArrayBooleanIterator(array);
    }
}
