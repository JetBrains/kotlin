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

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

@AssertInvisibleInResolver
public class Tuple0 {
    public static final Tuple0 VALUE = new Tuple0();

    private Tuple0() {
    }

    @Override
    public String toString() {
        return "Unit.VALUE";
    }

    @Override
    public boolean equals(Object o) {
        return o == VALUE;
    }

    @Override
    public int hashCode() {
        return 239;
    }
}
