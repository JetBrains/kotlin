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


public class LongProgression implements Progression<Long> {
    private final long start;
    private final long end;
    private final long increment;

    public LongProgression(long start, long end, long increment) {
        if (increment == 0) {
            throw new IllegalArgumentException("Increment must be non-zero: " + increment);
        }
        this.start = start;
        this.end = end;
        this.increment = increment;
    }

    @Override
    public Long getStart() {
        return start;
    }

    @Override
    public Long getEnd() {
        return end;
    }

    @Override
    public Long getIncrement() {
        return increment;
    }

    @Override
    public LongIterator iterator() {
        return new LongProgressionIterator(start, end, increment);
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

        LongProgression longs = (LongProgression) o;

        if (end != longs.end) return false;
        if (increment != longs.increment) return false;
        if (start != longs.start) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        result = 31 * result + (int) (increment ^ (increment >>> 32));
        return result;
    }
}
