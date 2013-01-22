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
public final class LongRange implements Range<Long>, LongIterable {
    private final long start;
    private final long count;

    public static final LongRange EMPTY = new LongRange(0L,0L);

    public LongRange(long startValue, long count) {
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


    public LongIterator step(long step) {
        if (step < 0)
            return new LongIteratorImpl(getEnd(), -count, -step);
        else
            return new LongIteratorImpl(start, count, step);
    }

    @Override
    public boolean contains(Long item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(long item) {
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

        LongRange range = (LongRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (count ^ (count >>> 32));
        return result;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1;
    }

    public long getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public LongIterator iterator() {
        return new LongIteratorImpl(start, count, 1);
    }

    private static class LongIteratorImpl extends LongIterator {
        private final long step;
        private long cur;
        private long count;

        private final boolean reversed;

        public LongIteratorImpl(long startValue, long count, long step) {
            cur = startValue;
            this.step = step;
            if (count < 0) {
                reversed = true;
                count = -count;
                startValue += count;
            }
            else {
                reversed = false;
            }
            this.count = count;
        }

        @Override
        public boolean hasNext() {
            return count > 0;
        }

        @Override
        public long nextLong() {
            count -= step;
            if (reversed) {
                cur -= step;
                return (cur + step);
            }
            else {
                cur += step;
                return (cur - step);
            }
        }
    }
}
