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

            val byCharCode = charCodes.associate { it to Char(it) }

            // 76=L only occurs once because only the last pair with the same key gets added
            assertPrints(byCharCode, "{72=H, 69=E, 76=L, 79=O}")
        }


        @Sample
        fun associateArrayOfPrimitivesBy() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)

            val byChar = charCodes.associateBy { Char(it) }

            // L=76 only occurs once because only the last pair with the same key gets added
            assertPrints(byChar, "{H=72, E=69, L=76, O=79}")
        }

        @Sample
        fun associateArrayOfPrimitivesByWithValueTransform() {
            val charCodes = intArrayOf(65, 65, 66, 67, 68, 69)

            val byUpperCase = charCodes.associateBy({ Char(it) }, { Char(it + 32) })

            // A=a only occurs once because only the last pair with the same key gets added
            assertPrints(byUpperCase, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateArrayOfPrimitivesByTo() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)
            val byChar = mutableMapOf<Char, Int>()

            assertTrue(byChar.isEmpty())
            charCodes.associateByTo(byChar) { Char(it) }

            assertTrue(byChar.isNotEmpty())
            // L=76 only occurs once because only the last pair with the same key gets added
            assertPrints(byChar, "{H=72, E=69, L=76, O=79}")
        }

        @Sample
        fun associateArrayOfPrimitivesByToWithValueTransform() {
            val charCodes = intArrayOf(65, 65, 66, 67, 68, 69)

            val byUpperCase = mutableMapOf<Char, Char>()
            charCodes.associateByTo(byUpperCase, { Char(it) }, { Char(it + 32) })

            // A=a only occurs once because only the last pair with the same key gets added
            assertPrints(byUpperCase, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateArrayOfPrimitivesTo() {
            val charCodes = intArrayOf(72, 69, 76, 76, 79)

            val byChar = mutableMapOf<Int, Char>()
            charCodes.associateTo(byChar) { it to Char(it) }

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
            val (even, odd) = array.partition { it % 2 == 0 }
            assertPrints(even, "[2, 4]")
            assertPrints(odd, "[1, 3, 5]")
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

        @Sample
        fun arrayContentEquals() {
            val array = arrayOf("apples", "oranges", "lime")

            // the same size and equal elements
            assertPrints(array.contentEquals(arrayOf("apples", "oranges", "lime")), "true")

            // different size
            assertPrints(array.contentEquals(arrayOf("apples", "oranges")), "false")

            // the elements at index 1 are not equal
            assertPrints(array.contentEquals(arrayOf("apples", "lime", "oranges")), "false")
        }

        @Sample
        fun charArrayContentEquals() {
            val array = charArrayOf('a', 'b', 'c')

            // the same size and equal elements
            assertPrints(array.contentEquals(charArrayOf('a', 'b', 'c')), "true")

            // different size
            assertPrints(array.contentEquals(charArrayOf('a', 'b')), "false")

            // the elements at index 1 are not equal
            assertPrints(array.contentEquals(charArrayOf('a', 'c', 'b')), "false")
        }

        @Sample
        fun booleanArrayContentEquals() {
            val array = booleanArrayOf(true, false, true)

            // the same size and equal elements
            assertPrints(array.contentEquals(booleanArrayOf(true, false, true)), "true")

            // different size
            assertPrints(array.contentEquals(booleanArrayOf(true, false)), "false")

            // the elements at index 1 are not equal
            assertPrints(array.contentEquals(booleanArrayOf(true, true, false)), "false")
        }

        @Sample
        fun intArrayContentEquals() {
            val array = intArrayOf(1, 2, 3)

            // the same size and equal elements
            assertPrints(array.contentEquals(intArrayOf(1, 2, 3)), "true")

            // different size
            assertPrints(array.contentEquals(intArrayOf(1, 2)), "false")

            // the elements at index 1 are not equal
            assertPrints(array.contentEquals(intArrayOf(1, 3, 2)), "false")
        }

        @Sample
        fun doubleArrayContentEquals() {
            val array = doubleArrayOf(1.0, Double.NaN, 0.0)

            // the same size and equal elements, NaN is equal to NaN
            assertPrints(array.contentEquals(doubleArrayOf(1.0, Double.NaN, 0.0)), "true")

            // different size
            assertPrints(array.contentEquals(doubleArrayOf(1.0, Double.NaN)), "false")

            // the elements at index 2 are not equal, 0.0 is not equal to -0.0
            assertPrints(array.contentEquals(doubleArrayOf(1.0, Double.NaN, -0.0)), "false")

            // the elements at index 1 are not equal
            assertPrints(array.contentEquals(doubleArrayOf(1.0, 0.0, Double.NaN)), "false")
        }

        @Sample
        fun contentDeepEquals() {
            val identityMatrix = arrayOf(
                intArrayOf(1, 0),
                intArrayOf(0, 1)
            )
            val reflectionMatrix = arrayOf(
                intArrayOf(1, 0),
                intArrayOf(0, -1)
            )

            // the elements at index [1][1] are not equal
            assertPrints(identityMatrix.contentDeepEquals(reflectionMatrix), "false")

            reflectionMatrix[1][1] = 1
            assertPrints(identityMatrix.contentDeepEquals(reflectionMatrix), "true")
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

        @Sample
        fun copyOfBooleanArrayWithInitializer() {
            val array = booleanArrayOf(true, false, true)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[true, false]")
            val paddedCopy = array.copyOf(5) { it % 2 == 0 }
            assertPrints(paddedCopy.contentToString(), "[true, false, true, false, true]")
        }

        @Sample
        fun copyOfCharArrayWithInitializer() {
            val array = charArrayOf('a', 'b', 'c')
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[a, b]")
            val paddedCopy = array.copyOf(5) { '?' }
            assertPrints(paddedCopy.contentToString(), "[a, b, c, ?, ?]")
        }

        @Sample
        fun copyOfByteArrayWithInitializer() {
            val array = byteArrayOf(1, 2, 3)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { -1 }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, -1, -1]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toByte() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfShortArrayWithInitializer() {
            val array = shortArrayOf(1, 2, 3)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { -1 }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, -1, -1]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toShort() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfIntArrayWithInitializer() {
            val array = intArrayOf(1, 2, 3)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { -1 }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, -1, -1]")
            val paddedCopyWithIndex = array.copyOf(6) { it }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfLongArrayWithInitializer() {
            val array = longArrayOf(1, 2, 3)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { -1 }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, -1, -1]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toLong() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfFloatArrayWithInitializer() {
            val array = floatArrayOf(1.0f, 2.0f, 3.0f)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1.0, 2.0]")
            val paddedCopy = array.copyOf(5) { -1.0f }
            assertPrints(paddedCopy.contentToString(), "[1.0, 2.0, 3.0, -1.0, -1.0]")
        }

        @Sample
        fun copyOfDoubleArrayWithInitializer() {
            val array = doubleArrayOf(1.0, 2.0, 3.0)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1.0, 2.0]")
            val paddedCopy = array.copyOf(5) { -1.0 }
            assertPrints(paddedCopy.contentToString(), "[1.0, 2.0, 3.0, -1.0, -1.0]")
        }

        @Sample
        fun copyOfArrayWithInitializer() {
            val array = arrayOf("foo", "bar", "baz")
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[foo, bar]")
            val paddedCopy = array.copyOf(5) { "qux" }
            assertPrints(paddedCopy.contentToString(), "[foo, bar, baz, qux, qux]")
        }

        @Sample
        fun copyOfUByteArrayWithInitializer() {
            val array = ubyteArrayOf(1u, 2u, 3u)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { 0xffu }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, 255, 255]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toUByte() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfUShortArrayWithInitializer() {
            val array = ushortArrayOf(1u, 2u, 3u)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { 0xffu }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, 255, 255]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toUShort() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfUIntArrayWithInitializer() {
            val array = uintArrayOf(1u, 2u, 3u)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { 0xffu }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, 255, 255]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toUInt() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
        }

        @Sample
        fun copyOfULongArrayWithInitializer() {
            val array = ulongArrayOf(1u, 2u, 3u)
            val truncatedCopy = array.copyOf(2)
            assertPrints(truncatedCopy.contentToString(), "[1, 2]")
            val paddedCopy = array.copyOf(5) { 0xffu }
            assertPrints(paddedCopy.contentToString(), "[1, 2, 3, 255, 255]")
            val paddedCopyWithIndex = array.copyOf(6) { it.toULong() }
            assertPrints(paddedCopyWithIndex.contentToString(), "[1, 2, 3, 3, 4, 5]")
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
