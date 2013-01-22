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
public final class CharRange implements Range<Character>, CharIterable {
    private final char start;
    private final int count;

    public static final CharRange EMPTY = new CharRange((char) 0,0);

    public CharRange(char startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public String toString() {
        if (count == 0) {
            return "<empty range>";
        }
        else if (count > 0) {
            return getStart() + ".rangeTo(" + getEnd() + ")";
        }
        else {
            return getStart() + ".downTo(" + getEnd() + ")";
        }
    }

    @Override
    public boolean contains(Character item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean contains(char item) {
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CharRange range = (CharRange) o;
        return count == range.count && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + count;
        return result;
    }

    public CharIterator step(int step) {
        if (step < 0)
            return new CharIteratorImpl(getEnd(), getStart(), step);
        else
            return new CharIteratorImpl(getStart(), getEnd(), step);
    }

    public char getStart() {
        return start;
    }

    public char getEnd() {
        return (char) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public CharIterator iterator() {
        return new CharIteratorImpl(getStart(), getEnd(), 1);
    }

    private static class CharIteratorImpl extends CharIterator {
        private char next;
        private final char end;
        private final int increment;

        public CharIteratorImpl(char start, char end, int increment) {
            this.next = start;
            this.end = end;
            this.increment = increment;
        }

        @Override
        public boolean hasNext() {
            return increment > 0 ? next <= end : next >= end;
        }

        @Override
        public char nextChar() {
            char value = next;
            next += increment;
            return value;
        }
    }
}
