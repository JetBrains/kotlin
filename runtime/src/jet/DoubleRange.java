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
public final class DoubleRange implements Range<Double> {
    private final double start;
    private final double size;

    public static final DoubleRange EMPTY = new DoubleRange(0, 0);

    public DoubleRange(double startValue, double size) {
        this.start = startValue;
        this.size = size;
    }

    @Override
    public String toString() {
        if (size == 0.0) {
            return "<empty range>";
        }
        else if (size > 0) {
            return getStart() + ".rangeTo(" + getEnd() + ")";
        }
        else {
            return getStart() + ".downTo(" + getEnd() + ")";
        }
    }

    @Override
    public boolean contains(Double item) {
        if (item == null) return false;
        if (size >= 0) {
            return item >= start && item < start + size;
        }
        return item <= start && item > start + size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DoubleRange range = (DoubleRange) o;

        return Double.compare(range.size, size) == 0 && Double.compare(range.start, start) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = start != +0.0d ? Double.doubleToLongBits(start) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = size != +0.0d ? Double.doubleToLongBits(size) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public DoubleIterator step(double step) {
        if (step < 0)
            return new DoubleIteratorImpl(getEnd(), -size, -step);
        else
            return new DoubleIteratorImpl(start, size, step);
    }

    public boolean getIsReversed() {
        return size < 0;
    }

    public double  getStart() {
        return start;
    }

    public double  getEnd() {
        return size < 0 ? start + size: start + size;
    }

    public double getSize() {
        return size < 0 ? -size : size;
    }

    private static class DoubleIteratorImpl extends DoubleIterator {
        private final double step;
        private final double end;
        private double cur;

        private final boolean reversed;

        public DoubleIteratorImpl(double startValue, double size, double step) {
            cur = startValue;
            this.step = step;
            if (size < 0) {
                reversed = true;
                end = startValue-size;
                startValue -= size;
            }
            else {
                reversed = false;
                this.end = startValue + size;
            }
        }

        @Override
        public boolean hasNext() {
            if (reversed)
                return cur >= end;
            else
                return cur <= end;
        }

        @Override
        public double nextDouble() {
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
