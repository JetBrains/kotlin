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

public final class SharedVar {
    public static final class Object<T> {
        public T ref;
    }
    
    public static final class Byte {
        public byte ref;
    }

    public static final class Short {
        public short ref;
    }

    public static final class Int {
        public int ref;
    }

    public static final class Long {
        public long ref;
    }

    public static final class Float {
        public float ref;
    }

    public static final class Double {
        public double ref;
    }

    public static final class Char {
        public char ref;
    }

    public static final class Boolean {
        public boolean ref;
    }
}
