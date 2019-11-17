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
        fun associate() {
            data class Person(val firstName: String, val lastName: String)

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = computerScientists.associate { it.lastName to it.firstName }

            assertPrints(byLastName, "{Hopper=Grace, Dijkstra=Edsger, Turing=Alan}")
        }

        @Sample
        fun associateArrayOfPrimitives() {
            fun fib(n: Int): Int {
                var current = 0
                var next = 1
                var sum: Int
                if (n == 0)
                    return current
                for (i in 2..n) {
                    sum = current + next
                    current = next
                    next = sum
                }
                return next
            }

            val numbers = intArrayOf(1, 2, 3, 4, 5)

            val fibonacciNumbers = numbers.associate { it to fib(it) }

            assertPrints(fibonacciNumbers, "{1=1, 2=1, 3=2, 4=3, 5=5}")
        }

        @Sample
        fun associateBy() {
            data class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = computerScientists.associateBy { it.lastName }

            assertPrints(byLastName.keys, "[Hopper, Dijkstra, Turing]")
            assertPrints(byLastName.values, "[Grace Hopper, Edsger Dijkstra, Alan Turing]")
        }

        @Sample
        fun associateArrayOfPrimitivesBy() {
            val asciiValues = intArrayOf(65, 66, 67, 68, 69)

            val byChar = asciiValues.associateBy { it.toChar() }

            assertPrints(byChar, "{A=65, B=66, C=67, D=68, E=69}")
        }

        @Sample
        fun associateByWithValueTransform() {
            data class Person(val firstName: String, val lastName: String)

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = computerScientists.associateBy({ it.lastName }, { it.firstName })

            assertPrints(byLastName, "{Hopper=Grace, Dijkstra=Edsger, Turing=Alan}")
        }

        @Sample
        fun associateArrayOfPrimitivesByWithValueTransform() {
            val asciiValues = intArrayOf(65, 66, 67, 68, 69)

            val byCharToLowerCase = asciiValues.associateBy({ it.toChar() }, { (it + 32).toChar() })

            assertPrints(byCharToLowerCase, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateByTo() {
            data class Person(val firstName: String, val lastName: String) {
                override fun toString(): String = "$firstName $lastName"
            }

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = mutableMapOf<String, Person>()
            assertTrue(byLastName.isEmpty())

            computerScientists.associateByTo(byLastName) { it.lastName }

            assertTrue(byLastName.isNotEmpty())
            assertPrints(byLastName.keys, "[Hopper, Dijkstra, Turing]")
            assertPrints(byLastName.values, "[Grace Hopper, Edsger Dijkstra, Alan Turing]")
        }

        @Sample
        fun associateArrayOfPrimitivesByTo() {
            val asciiValues = intArrayOf(65, 66, 67, 68, 69)

            val byChar = mutableMapOf<Char, Int>()
            asciiValues.associateByTo(byChar) { it.toChar() }

            assertPrints(byChar, "{A=65, B=66, C=67, D=68, E=69}")
        }

        @Sample
        fun associateByToWithValueTransform() {
            data class Person(val firstName: String, val lastName: String)

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = mutableMapOf<String, String>()
            assertTrue(byLastName.isEmpty())

            computerScientists.associateByTo(byLastName, { it.lastName }, { it.firstName} )

            assertTrue(byLastName.isNotEmpty())
            assertPrints(byLastName, "{Hopper=Grace, Dijkstra=Edsger, Turing=Alan}")
        }

        @Sample
        fun associateArrayOfPrimitivesByToWithValueTransform() {
            val asciiValues = intArrayOf(65, 66, 67, 68, 69)

            val byChar = mutableMapOf<Char, Char>()
            asciiValues.associateByTo(byChar, { it.toChar() }, { (it + 32).toChar() } )

            assertPrints(byChar, "{A=a, B=b, C=c, D=d, E=e}")
        }

        @Sample
        fun associateTo() {
            data class Person(val firstName: String, val lastName: String)

            val computerScientists = arrayOf(Person("Grace", "Hopper"), Person("Edsger", "Dijkstra"), Person("Alan", "Turing"))

            val byLastName = mutableMapOf<String, String>()
            assertTrue(byLastName.isEmpty())

            computerScientists.associateTo(byLastName) { it.lastName to it.firstName }

            assertTrue(byLastName.isNotEmpty())
            assertPrints(byLastName, "{Hopper=Grace, Dijkstra=Edsger, Turing=Alan}")
        }

        @Sample
        fun associateArrayOfPrimitivesTo() {
            fun fib(n: Int, memo: MutableMap<Int, Int>): Int {
                val fib = memo[n] ?: fib(n - 1, memo) + fib(n - 2, memo)
                memo[n] = fib
                return fib
            }

            val numbers = intArrayOf(22, 23, 24, 25, 26)

            val fibonacciNumbers = mutableMapOf(0 to 0, 1 to 1)

            numbers.associateTo(fibonacciNumbers) { it to fib(it, fibonacciNumbers) }

            assertPrints(fibonacciNumbers[25], "75025")
            assertPrints(fibonacciNumbers[26], "121393")
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