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
        fun associateArrayOfPrimitives() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)

            val byCharCode = charCodes.associate { it to it.toChar() }

            // 76=L only occurs once because only the last pair with the same key gets added
            assertPrints(byCharCode, "{72=H, 69=E, 76=L, 79=O}")
        }


        @Sample
        fun associateArrayOfPrimitivesBy() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)

            val byChar = charCodes.associateBy { it.toChar() }

            // L=76 only occurs once because only the last pair with the same key gets added
            assertPrints(byChar, "{H=72, E=69, L=76, O=79}")
        }

        @Sample
        fun associateArrayOfPrimitivesByWithValueTransform() {
            val charCodes = intArrayOf(65, 65, 66, 67, 68, 69)

            val byUpperCase = charCodes.associateBy({ it.toChar() }, { (it + 32).toChar() })

            // A=a only occurs once because only the last pair with the same key gets added
            assertPrints(byUpperCase, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateArrayOfPrimitivesByTo() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)
            val byChar = mutableMapOf<Char, Int>()

            assertTrue(byChar.isEmpty())
            charCodes.associateByTo(byChar) { it.toChar() }

            assertTrue(byChar.isNotEmpty())
            // L=76 only occurs once because only the last pair with the same key gets added
            assertPrints(byChar, "{H=72, E=69, L=76, O=79}")
        }

        @Sample
        fun associateArrayOfPrimitivesByToWithValueTransform() {
            val charCodes = intArrayOf(65, 65, 66, 67, 68, 69)

            val byUpperCase = mutableMapOf<Char, Char>()
            charCodes.associateByTo(byUpperCase, { it.toChar() }, { (it + 32).toChar() } )

            // A=a only occurs once because only the last pair with the same key gets added
            assertPrints(byUpperCase, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateArrayOfPrimitivesTo() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)

            val byChar = mutableMapOf<Int, Char>()
            charCodes.associateTo(byChar) { it to it.toChar() }

            // 76=L only occurs once because only the last pair with the same key gets added
            assertPrints(byChar, "{72=H, 69=E, 76=L, 79=O}")
        }

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

        @Sample
        fun partitionArrayOfPrimitives() {
            val array = intArrayOf(1, 2, 3, 4, 5)
            val partition = array.partition { it % 2 == 0 }
            assertPrints(partition, "([2, 4], [1, 3, 5])")
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

    class Sorting {

        @Sample
        fun sortArray() {
            val intArray = intArrayOf(4, 3, 2, 1)

            // before sorting
            assertPrints(intArray.joinToString(), "4, 3, 2, 1")

            intArray.sort()

            // after sorting
            assertPrints(intArray.joinToString(), "1, 2, 3, 4")
        }

        @Sample
        fun sortArrayOfComparable() {
            class Person(val firstName: String, val lastName: String) : Comparable<Person> {
                override fun compareTo(other: Person): Int = this.lastName.compareTo(other.lastName)
                override fun toString(): String = "$firstName $lastName"
            }

            val people = arrayOf(
                Person("Ragnar", "Lodbrok"),
                Person("Bjorn", "Ironside"),
                Person("Sweyn", "Forkbeard")
            )

            // before sorting
            assertPrints(people.joinToString(), "Ragnar Lodbrok, Bjorn Ironside, Sweyn Forkbeard")

            people.sort()

            // after sorting
            assertPrints(people.joinToString(), "Sweyn Forkbeard, Bjorn Ironside, Ragnar Lodbrok")

        }

        @Sample
        fun sortRangeOfArray() {
            val intArray = intArrayOf(4, 3, 2, 1)

            // before sorting
            assertPrints(intArray.joinToString(), "4, 3, 2, 1")

            intArray.sort(0, 3)

            // after sorting
            assertPrints(intArray.joinToString(), "2, 3, 4, 1")
        }

        @Sample
        fun sortRangeOfArrayOfComparable() {
            class Person(val firstName: String, val lastName: String) : Comparable<Person> {
                override fun compareTo(other: Person): Int = this.lastName.compareTo(other.lastName)
                override fun toString(): String = "$firstName $lastName"
            }

            val people = arrayOf(
                Person("Ragnar", "Lodbrok"),
                Person("Bjorn", "Ironside"),
                Person("Sweyn", "Forkbeard")
            )

            // before sorting
            assertPrints(people.joinToString(), "Ragnar Lodbrok, Bjorn Ironside, Sweyn Forkbeard")

            people.sort(0, 2)

            // after sorting
            assertPrints(people.joinToString(), "Bjorn Ironside, Ragnar Lodbrok, Sweyn Forkbeard")
        }

    }

}