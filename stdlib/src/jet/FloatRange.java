/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

public final class FloatRange implements Range<Float>, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(FloatRange.class, false);

    private final float start;
    private final float size;

    public FloatRange(float startValue, float size) {
        this.start = startValue;
        this.size = size;
    }

    @Override
    public boolean contains(Float item) {
        if (item == null) return false;
        if (size >= 0) {
            return item >= start && item < start + size;
        }
        return item <= start && item > start + size;
    }

    public FloatIterator step(float step) {
        if(step < 0)
            return new MyIterator(getEnd(), -size, -step);
        else
            return new MyIterator(start, size, step);
    }

    public boolean getIsReversed() {
        return size < 0;
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

    public FloatRange minus() {
        return new FloatRange(getEnd(), -size);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static FloatRange count(int length) {
        return new FloatRange(0, length);
    }

    private static class MyIterator extends FloatIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private float cur;
        private float step;
        private final float end;

        private final boolean reversed;

        public MyIterator(float startValue, float size, float step) {
            cur = startValue;
            this.step = step;
            if(size < 0) {
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
        public boolean getHasNext() {
            if(reversed)
                return cur >= end;
            else
                return cur <= end;
        }

        @Override
        public float nextFloat() {
            if(reversed) {
                cur -= step;
                return cur + step;
            }
            else {
                cur += step;
                return cur - step;
            }
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
}
