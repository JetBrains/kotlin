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

import jet.runtime.ProgressionUtil;

class CharProgressionIterator extends CharIterator {
    private int next;
    private final int increment;
    private final char finalElement;
    private boolean hasNext;

    public CharProgressionIterator(char start, char end, int increment) {
        this.next = start;
        this.increment = increment;

        this.finalElement = (char) ProgressionUtil.getProgressionFinalElement(start, end, increment);
        this.hasNext = increment > 0 ? start <= end : start >= end;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public char nextChar() {
        int value = next;
        if (value == finalElement) {
            hasNext = false;
        }
        else {
            next += increment;
        }
        return (char) value;
    }
}
