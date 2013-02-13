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
public class DoubleProgression implements Progression<Double> {
    private final double start;
    private final double end;
    private final double increment;

    public DoubleProgression(double start, double end, double increment) {
        if (Double.isNaN(increment)) {
            throw new IllegalArgumentException("Increment must be not NaN");
        }
        if (increment == 0.0 || increment == -0.0) {
            throw new IllegalArgumentException("Increment must be non-zero: " + increment);
        }
        this.start = start;
        this.end = end;
        this.increment = increment;
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
        return increment;
    }

    @Override
    public DoubleIterator iterator() {
        return new DoubleProgressionIterator(start, end, increment);
    }

    @Override
    public String toString() {
        if (increment > 0) {
            return start + ".." + end + " step " + increment;
        }
        else {
            return start + " downTo " + end + " step " + -increment;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleProgression doubles = (DoubleProgression) o;

        if (Double.compare(doubles.end, end) != 0) return false;
        if (Double.compare(doubles.increment, increment) != 0) return false;
        if (Double.compare(doubles.start, start) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = start != +0.0d ? Double.doubleToLongBits(start) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = end != +0.0d ? Double.doubleToLongBits(end) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = increment != +0.0d ? Double.doubleToLongBits(increment) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
