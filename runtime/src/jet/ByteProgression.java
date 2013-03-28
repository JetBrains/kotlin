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


public class ByteProgression implements Progression<Byte> {
    private final byte start;
    private final byte end;
    private final int increment;

    public ByteProgression(byte start, byte end, int increment) {
        if (increment == 0) {
            throw new IllegalArgumentException("Increment must be non-zero: " + increment);
        }
        this.start = start;
        this.end = end;
        this.increment = increment;
    }

    @Override
    public Byte getStart() {
        return start;
    }

    @Override
    public Byte getEnd() {
        return end;
    }

    @Override
    public Integer getIncrement() {
        return increment;
    }

    @Override
    public ByteIterator iterator() {
        return new ByteProgressionIterator(start, end, increment);
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

        ByteProgression bytes = (ByteProgression) o;

        if (end != bytes.end) return false;
        if (increment != bytes.increment) return false;
        if (start != bytes.start) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + (int) end;
        result = 31 * result + increment;
        return result;
    }
}
