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

import samples.*
import kotlin.coroutines.experimental.buildIterator
import kotlin.test.*

class Iterables {

    @Sample
    fun iterable() {
        val iterable = Iterable { buildIterator { yieldAll(1..3) } }
        val result = iterable.mapIndexed { index, i -> index + i }
        assertPrints(result, "[1, 3, 5]")
    }

    @Sample
    fun flattenIterable() {
        val deepList = listOf(listOf(1), listOf(2, 3), listOf(4, 5, 6))
        assertPrints(deepList.flatten(), "[1, 2, 3, 4, 5, 6]")
    }

    @Sample
    fun unzipIterable() {
        val list = listOf(1 to 'a', 2 to 'b', 3 to 'c')
        assertPrints(list.unzip(), "([1, 2, 3], [a, b, c])")
    }
}