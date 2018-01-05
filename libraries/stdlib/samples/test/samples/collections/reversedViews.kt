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
        val original = ('a'..'e').toList()
        val reversed = original.asReversed()

        assertPrints(original, "[a, b, c, d, e]")
        assertPrints(reversed, "[e, d, c, b, a]")
    }

    @Sample
    fun asReversedMutableList() {
        val original = mutableListOf(1, 2, 3, 4, 5)
        val reversed = original.asReversed()

        assertPrints(original, "[1, 2, 3, 4, 5]")
        assertPrints(reversed, "[5, 4, 3, 2, 1]")

        original.add(6)
        assertPrints(original, "[1, 2, 3, 4, 5, 6]")
        assertPrints(reversed, "[6, 5, 4, 3, 2, 1]")

        original.add(0, 0)
        assertPrints(original, "[0, 1, 2, 3, 4, 5, 6]")
        assertPrints(reversed, "[6, 5, 4, 3, 2, 1, 0]")

        original[original.lastIndex] = 10
        assertPrints(original, "[0, 1, 2, 3, 4, 5, 10]")
        assertPrints(reversed, "[10, 5, 4, 3, 2, 1, 0]")

        original.removeAt(original.lastIndex)
        assertPrints(original, "[0, 1, 2, 3, 4, 5]")
        assertPrints(reversed, "[5, 4, 3, 2, 1, 0]")

        original.remove(5)
        assertPrints(original, "[0, 1, 2, 3, 4]")
        assertPrints(reversed, "[4, 3, 2, 1, 0]")
    }
}