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


public final class CharRange implements Range<Character>, Progression<Character> {
    public static final CharRange EMPTY = new CharRange((char) 1, (char) 0);

    private final char start;
    private final char end;

    public CharRange(char start, char end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Character item) {
        return start <= item && item <= end;
    }

    public boolean contains(char item) {
        return start <= item && item <= end;
    }

    @Override
    public Character getStart() {
        return start;
    }

    @Override
    public Character getEnd() {
        return end;
    }

    @Override
    public Integer getIncrement() {
        return 1;
    }

    @Override
    public CharIterator iterator() {
        return new CharProgressionIterator(start, end, 1);
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

        CharRange range = (CharRange) o;
        return end == range.end && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + end;
        return result;
    }
}
