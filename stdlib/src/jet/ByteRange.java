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

public final class ByteRange implements Range<Byte>, ByteIterable, JetObject {
    private final byte start;
    private final int count;

    public static final ByteRange empty = new ByteRange((byte) 0,0);

    public ByteRange(byte startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public boolean contains(Byte item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public byte getStart() {
        return start;
    }

    public byte getIteratorStart () {
        return count == 0 ? 1 : start;
    }

    public byte getEnd() {
        return (byte) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public ByteIterator step(int step) {
        if(step < 0)
            return new MyIterator(getEnd(), -count, -step);
        else
            return new MyIterator(start, count, step);
    }

    public ByteRange minus() {
        return new ByteRange(getEnd(), -count);
    }

    @Override
    public ByteIterator iterator() {
        return new MyIterator(start, count, 1);
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static ByteRange count(int length) {
        return new ByteRange((byte) 0, length);
    }

    private static class MyIterator extends ByteIterator {
        private byte cur;
        private int step;
        private int count;

        private final boolean reversed;

        public MyIterator(byte startValue, int count, int step) {
            cur = startValue;
            this.step = step;
            if(count < 0) {
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
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public byte nextByte() {
            count -= step;
            if(reversed) {
                cur -= step;
                return (byte) (cur + step);
            }
            else {
                cur += step;
                return (byte) (cur - step);
            }
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
}
