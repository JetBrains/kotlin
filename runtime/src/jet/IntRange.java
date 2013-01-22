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
public final class IntRange implements Range<Integer>, IntIterable {
    private final int start;
    private final int count;

    public static final IntRange EMPTY = new IntRange(0, 0);

    public IntRange(int startValue, int count) {
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
    public boolean contains(Integer item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(int item) {
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

        IntRange range = (IntRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = start;
        result = 31 * result + count;
        return result;
    }

    public IntIterator step(int step) {
        if (step < 0)
            return new IntIteratorImpl(getEnd(), -count, -step);
        else
            return new IntIteratorImpl(start, count, step);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1;
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public IntIterator iterator() {
        return new IntIteratorImpl(start, count, 1);
    }

    private static class IntIteratorImpl extends IntIterator {
        private int cur;
        private int step;
        private int count;

        private final boolean reversed;

        public IntIteratorImpl(int startValue, int count, int step) {
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
        public int nextInt() {
            count -= step;
            if (reversed) {
                cur -= step;
                return cur + step;
            }
            else {
                cur += step;
                return cur - step;
            }
        }
    }
}
