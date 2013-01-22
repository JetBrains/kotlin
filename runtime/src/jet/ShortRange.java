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
public final class ShortRange implements Range<Short>, ShortIterable {
    private final short start;
    private final int count;

    public static final ShortRange EMPTY = new ShortRange((short) 0,0);

    public ShortRange(short startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public String toString() {
        if (count == 0) {
            return "<empty range>";
        }
        else if (count > 0) {
            return getStart() + ".rangeTo(" + getEnd() + ")";
        }
        else {
            return getStart() + ".downTo(" + getEnd() + ")";
        }
    }

    @Override
    public boolean contains(Short item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(short item) {
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShortRange range = (ShortRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + count;
        return result;
    }

    public ShortIterator step(int step) {
        if (step < 0)
            return new ShortIteratorImpl(getEnd(), getStart(), step);
        else
            return new ShortIteratorImpl(getStart(), getEnd(), step);
    }

    public short getStart() {
        return start;
    }

    public short getEnd() {
        return (short) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public ShortIterator iterator() {
        return new ShortIteratorImpl(getStart(), getEnd(), 1);
    }

    private static class ShortIteratorImpl extends ShortIterator {
        private short next;
        private final short end;
        private final int increment;

        public ShortIteratorImpl(short start, short end, int increment) {
            this.next = start;
            this.end = end;
            this.increment = increment;
        }

        @Override
        public boolean hasNext() {
            return increment > 0 ? next <= end : next >= end;
        }

        @Override
        public short nextShort() {
            short value = next;
            next += increment;
            return value;
        }
    }
}
