/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package jet;

public class Tuple1<T1> extends Tuple {
    public final T1 _1;

    public Tuple1(T1 t1) {
        _1 = t1;
    }

    @Override
    public String toString() {
        return "(" + _1 + ")";
    }
    public final T1 get_1() {
        return _1;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple1 t = (Tuple1) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        return result;
    }

    @Override
    public void forEach(Function1<Object, Void> fn) {
        fn.invoke(_1);
    }

    @Override
    public int size() {
        return 1;
    }
}
