/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package samples.collections

import samples.Sample
import samples.assertPrints

class ReversedViews {
    @Sample
    fun asReversedList() {
        val original = mutableListOf('a', 'b', 'c', 'd', 'e')
        val originalReadOnly = original as List<Char>
        val reversed = originalReadOnly.asReversed()

        assertPrints(original, "[a, b, c, d, e]")
        assertPrints(reversed, "[e, d, c, b, a]")

        original.add(original.size, 'f')
        assertPrints(original, "[a, b, c, d, e, f]")
        assertPrints(reversed, "[f, e, d, c, b, a]")

        original.set(original.lastIndex, 'z')
        assertPrints(original, "[a, b, c, d, e, z]")
        assertPrints(reversed, "[z, e, d, c, b, a]")
    }

    @Sample
    fun asReversedMutableList() {
        val original = mutableListOf(1, 2, 3, 4, 5)
        val reversed = original.asReversed()

        assertPrints(original, "[1, 2, 3, 4, 5]")
        assertPrints(reversed, "[5, 4, 3, 2, 1]")

        reversed.add(reversed.size, 0)
        assertPrints(original, "[0, 1, 2, 3, 4, 5]")
        assertPrints(reversed, "[5, 4, 3, 2, 1, 0]")

        reversed.set(0, 10)
        assertPrints(original, "[0, 1, 2, 3, 4, 10]")
        assertPrints(reversed, "[10, 4, 3, 2, 1, 0]")
    }
}