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

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

@AssertInvisibleInResolver
public final class ByteRange implements Range<Byte>, NumberSequence<Byte> {
    public static final ByteRange EMPTY = new ByteRange((byte) 1, (byte) 0);

    private final byte start;
    private final byte end;

    public ByteRange(byte start, byte end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Byte item) {
        return start <= item && item <= end;
    }

    public boolean contains(byte item) {
        return start <= item && item <= end;
    }

    @Override
    public Byte getStart() {
        return start;
    }

    @Override
    public Byte getEnd() {
        return end;
    }

    @Override
    public Integer getIncrement() {
        return 1;
    }

    @Override
    public ByteIterator iterator() {
        return new ByteSequenceIterator(start, end, 1);
    }

    @Override
    public String toString() {
        return start + ".." + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ByteRange range = (ByteRange) o;
        return end == range.end && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + end;
        return result;
    }
}
