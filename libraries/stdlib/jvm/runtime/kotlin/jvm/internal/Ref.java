/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import java.io.Serializable;

public class Ref {
    private Ref() {}

    public static final class ObjectRef<T> implements Serializable {
        public T element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class ByteRef implements Serializable {
        public byte element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class ShortRef implements Serializable {
        public short element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class IntRef implements Serializable {
        public int element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class LongRef implements Serializable {
        public long element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class FloatRef implements Serializable {
        public float element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class DoubleRef implements Serializable {
        public double element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class CharRef implements Serializable {
        public char element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }

    public static final class BooleanRef implements Serializable {
        public boolean element;

        @Override
        public String toString() {
            return String.valueOf(element);
        }
    }
}
