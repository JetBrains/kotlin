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

/**
 * Represents a string template object; that is a string with $ expressions such as "Hello $user".
 *
 * It is represented as an object that contains a Tuple
 */
public class StringTemplate {
    private final Tuple tuple;

    public StringTemplate(Tuple tuple) {
        this.tuple = tuple;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StringTemplate that = (StringTemplate) o;

        if (tuple != null ? !tuple.equals(that.tuple) : that.tuple != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return tuple != null ? tuple.hashCode() : 0;
    }

    /**
     * Returns the plain string version of the string template with no special escaping
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        tuple.forEach(new Function1<Object,Tuple0>(){
            @Override
            public Tuple0 invoke(Object o) {
                builder.append(o);
                return Tuple0.INSTANCE;
            }
        });
        return builder.toString();

    }

    /**
     * Returns the tuple of values in the string template
     */
    public Tuple getValues() {
        return tuple;
    }


    /**
     * Performs the given function on each value in the string template
     */
    public void forEach(Function1<Object, Tuple0> fn) {
        if (tuple != null) {
            tuple.forEach(fn);
        }
    }

}
