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
import java.util.*

class Iterators {

    @Sample
    fun iteratorForEnumeration() {
        val list = arrayListOf<String>()
        val vector = Vector<String>().apply {
            add("RED")
            add("GREEN")
            add("BLUE")
        }
        val iterator = vector.elements().iterator()
        while (iterator.hasNext()) {
            list.add(iterator.next())
        }

        assertPrints(list.joinToString(":"), "RED:GREEN:BLUE")
    }

    @Sample
    fun iterator() {
        val intIterator = IntProgression.fromClosedRange(1, 3, 1).iterator()
        val iterator = intIterator.iterator()

        assertPrints(iterator.next(), "1")
        assertPrints(iterator.next(), "2")
        assertPrints(iterator.next(), "3")
    }

    @Sample
    fun withIndexIterator() {
        var result = 0
        val iterator = (1..3).iterator()
        for ((index) in iterator.withIndex()) {
            result += index
        }

        assertPrints(result, "3")
    }

    @Sample
    fun forEachIterator() {
        var result = 0
        (1..3).iterator().forEach {
            result += it
        }

        assertPrints(result, "6")
    }
}