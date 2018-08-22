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
import kotlin.test.*


@RunWith(Enclosed::class)
class Arrays {

    class Usage {

        @Sample
        fun arrayOrEmpty() {
            val nullArray: Array<Any>? = null
            assertPrints(nullArray.orEmpty().contentToString(), "[]")

            val array: Array<Char>? = arrayOf('a', 'b', 'c')
            assertPrints(array.orEmpty().contentToString(), "[a, b, c]")
        }

        @Sample
        fun arrayIsNullOrEmpty() {
            val nullArray: Array<Any>? = null
            assertTrue(nullArray.isNullOrEmpty())

            val emptyArray: Array<Any>? = emptyArray<Any>()
            assertTrue(emptyArray.isNullOrEmpty())

            val array: Array<Char>? = arrayOf('a', 'b', 'c')
            assertFalse(array.isNullOrEmpty())
        }

        @Sample
        fun arrayIfEmpty() {
            val emptyArray: Array<Any> = emptyArray()

            val emptyOrNull: Array<Any>? = emptyArray.ifEmpty { null }
            assertPrints(emptyOrNull, "null")

            val emptyOrDefault: Array<Any> = emptyArray.ifEmpty { arrayOf("default") }
            assertPrints(emptyOrDefault.contentToString(), "[default]")

            val nonEmptyArray = arrayOf(1)
            val sameArray = nonEmptyArray.ifEmpty { arrayOf(2) }
            assertTrue(nonEmptyArray === sameArray)
        }
    }

    class Transformations {

        @Sample
        fun flattenArray() {
            val deepArray = arrayOf(
                arrayOf(1),
                arrayOf(2, 3),
                arrayOf(4, 5, 6)
            )

            assertPrints(deepArray.flatten(), "[1, 2, 3, 4, 5, 6]")
        }

        @Sample
        fun unzipArray() {
            val array = arrayOf(1 to 'a', 2 to 'b', 3 to 'c')
            assertPrints(array.unzip(), "([1, 2, 3], [a, b, c])")
        }
    }

    class ContentOperations {

        @Sample
        fun contentToString() {
            val array = arrayOf("apples", "oranges", "lime")

            assertPrints(array.contentToString(), "[apples, oranges, lime]")
        }

        @Sample
        fun contentDeepToString() {
            val matrix = arrayOf(
                intArrayOf(3, 7, 9),
                intArrayOf(0, 1, 0),
                intArrayOf(2, 4, 8)
            )

            assertPrints(matrix.contentDeepToString(), "[[3, 7, 9], [0, 1, 0], [2, 4, 8]]")
        }
    }

    class CopyOfOperations {

        @Sample
        fun copyOf() {
            val array = arrayOf("apples", "oranges", "limes")
            val arrayCopy = array.copyOf()
            assertPrints(arrayCopy.contentToString(), "[apples, oranges, limes]")
        }

        @Sample
        fun resizingCopyOf() {
            val array = arrayOf("apples", "oranges", "limes")
            val arrayCopyPadded = array.copyOf(5)
            assertPrints(arrayCopyPadded.contentToString(), "[apples, oranges, limes, null, null]")
            val arrayCopyTruncated = array.copyOf(2)
            assertPrints(arrayCopyTruncated.contentToString(), "[apples, oranges]")
        }

        @Sample
        fun resizedPrimitiveCopyOf() {
            val array = intArrayOf(1, 2, 3)
            val arrayCopyPadded = array.copyOf(5)
            assertPrints(arrayCopyPadded.contentToString(), "[1, 2, 3, 0, 0]")
            val arrayCopyTruncated = array.copyOf(2)
            assertPrints(arrayCopyTruncated.contentToString(), "[1, 2]")
        }
    }

}