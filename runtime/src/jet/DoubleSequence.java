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
public class DoubleSequence implements NumberSequence<Double> {
    private final double start;
    private final double end;
    private final double increment;

    public DoubleSequence(double start, double end, double increment) {
        if (increment == 0.0 || increment == -0.0) {
            throw new IllegalArgumentException("Increment must be non-zero: " + increment);
        }
        this.start = start;
        this.end = end;
        this.increment = increment;
    }

    @Override
    public Double getStart() {
        return start;
    }

    @Override
    public Double getEnd() {
        return end;
    }

    @Override
    public Double getIncrement() {
        return increment;
    }

    @Override
    public DoubleIterator iterator() {
        return new DoubleSequenceIterator(start, end, increment);
    }
}
