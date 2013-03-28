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

package jet;

import java.util.Iterator;

public abstract class ByteIterator implements Iterator<Byte> {
    @Override
    public final Byte next() {
        return nextByte();
    }

    public abstract byte nextByte();

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Mutating method called on a Kotlin Iterator");
    }
}
