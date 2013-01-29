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
public final class DoubleRange implements Range<Double>, Progression<Double> {
    public static final DoubleRange EMPTY = new DoubleRange(1, 0);

    private final double start;
    private final double end;

    public DoubleRange(double start, double end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Double item) {
        return start <= item && item <= end;
    }

    public boolean contains(double item) {
        return start <= item && item <= end;
    }

    @Override
    public Double getStart() {
        return start;
    }

    @Override
    public Double getEnd() {
        return end;
    }

    @Override
    public Double getIncrement() {
        return 1.0;
    }

    @Override
    public DoubleIterator iterator() {
        return new DoubleProgressionIterator(start, end, 1.0);
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

        DoubleRange range = (DoubleRange) o;

        return Double.compare(range.end, end) == 0 && Double.compare(range.start, start) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = start != +0.0d ? Double.doubleToLongBits(start) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = end != +0.0d ? Double.doubleToLongBits(end) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
