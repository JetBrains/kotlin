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
public final class FloatRange implements Range<Float> {
    private final float start;
    private final float size;

    public static final FloatRange EMPTY = new FloatRange(0, 0);

    public FloatRange(float startValue, float size) {
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
    public boolean contains(Float item) {
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

        FloatRange range = (FloatRange) o;

        return Float.compare(range.size, size) == 0 && Float.compare(range.start, start) == 0;
    }

    @Override
    public int hashCode() {
        int result = (start != +0.0f ? Float.floatToIntBits(start) : 0);
        result = 31 * result + (size != +0.0f ? Float.floatToIntBits(size) : 0);
        return result;
    }

    public float  getStart() {
        return start;
    }

    public float  getEnd() {
        return size < 0 ? start + size: start + size;
    }

    public float getSize() {
        return size < 0 ? -size : size;
    }
}
