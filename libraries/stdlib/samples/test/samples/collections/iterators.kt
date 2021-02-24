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
import java.util.*

class Iterators {

    @Sample
    fun iteratorForEnumeration() {
        val vector = Vector<String>().apply {
            add("RED")
            add("GREEN")
            add("BLUE")
        }

        // iterator() extension is called here
        for (e in vector.elements()) {
            println("The element is $e")
        }
    }

    @Sample
    fun iterator() {
        val mutableList = mutableListOf(1, 2, 3)
        val mutableIterator = mutableList.iterator()

        // iterator() extension is called here
        for (e in mutableIterator) {
            if (e % 2 == 0) {
                // we can remove items from the iterator without getting ConcurrentModificationException
                // because it's the same iterator that is iterated with for loop
                mutableIterator.remove()
            }

            println("The element is $e")
        }
    }

    @Sample
    fun withIndexIterator() {
        val iterator = ('a'..'c').iterator()

        for ((index, value) in iterator.withIndex()) {
            println("The element at $index is $value")
        }
    }

    @Sample
    fun forEachIterator() {
        val iterator = (1..3).iterator()
        // skip an element
        if (iterator.hasNext()) {
            iterator.next()
        }

        // do something with the rest of elements
        iterator.forEach {
            println("The element is $it")
        }
    }
}