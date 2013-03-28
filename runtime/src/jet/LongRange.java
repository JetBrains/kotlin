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


public final class LongRange implements Range<Long>, Progression<Long> {
    public static final LongRange EMPTY = new LongRange(1, 0);

    private final long start;
    private final long end;

    public LongRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Long item) {
        return start <= item && item <= end;
    }

    public boolean contains(long item) {
        return start <= item && item <= end;
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
        return 1L;
    }

    @Override
    public LongIterator iterator() {
        return new LongProgressionIterator(start, end, 1);
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

        LongRange range = (LongRange) o;
        return end == range.end && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        return result;
    }
}
