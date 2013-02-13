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
public class FloatProgression implements Progression<Float> {
    private final float start;
    private final float end;
    private final float increment;

    public FloatProgression(float start, float end, float increment) {
        if (Float.isNaN(increment)) {
            throw new IllegalArgumentException("Increment must be not NaN");
        }
        if (increment == 0.0f || increment == -0.0f) {
            throw new IllegalArgumentException("Increment must be non-zero: " + increment);
        }
        this.start = start;
        this.end = end;
        this.increment = increment;
    }

    @Override
    public Float getStart() {
        return start;
    }

    @Override
    public Float getEnd() {
        return end;
    }

    @Override
    public Float getIncrement() {
        return increment;
    }

    @Override
    public FloatIterator iterator() {
        return new FloatProgressionIterator(start, end, increment);
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

        FloatProgression floats = (FloatProgression) o;

        if (Float.compare(floats.end, end) != 0) return false;
        if (Float.compare(floats.increment, increment) != 0) return false;
        if (Float.compare(floats.start, start) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (start != +0.0f ? Float.floatToIntBits(start) : 0);
        result = 31 * result + (end != +0.0f ? Float.floatToIntBits(end) : 0);
        result = 31 * result + (increment != +0.0f ? Float.floatToIntBits(increment) : 0);
        return result;
    }
}
