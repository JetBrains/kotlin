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

public final class DoubleRange implements Range<Double>, JetObject {
    private final double start;
    private final double size;

    public DoubleRange(double startValue, double size) {
        this.start = startValue;
        this.size = size;
    }

    @Override
    public boolean contains(Double item) {
        if (item == null) return false;
        if (size >= 0) {
            return item >= start && item < start + size;
        }
        return item <= start && item > start + size;
    }

    public DoubleIterator step(double step) {
        if(step < 0)
            return new MyIterator(getEnd(), -size, -step);
        else
            return new MyIterator(start, size, step);
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

    public DoubleRange minus() {
        return new DoubleRange(getEnd(), -size);
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static DoubleRange count(int length) {
        return new DoubleRange(0, length);
    }

    private static class MyIterator extends DoubleIterator {
        private double cur;
        private double step;
        private final double end;

        private final boolean reversed;

        public MyIterator(double startValue, double size, double step) {
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
        public double nextDouble() {
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
        public JetObject getOuterObject() {
            return null;
        }
    }
}
