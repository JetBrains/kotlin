package samples.collections

import samples.*
import kotlin.test.*

@RunWith(Enclosed::class)
class Sequences {

    class Building {

        @Sample
        fun generateSequence() {
            var count = 3

            val sequence = generateSequence {
                (count--).takeIf { it > 0 } // will return null, when value becomes non-positive,
                                            // and that will terminate the sequence
            }

            assertPrints(sequence.toList(), "[3, 2, 1]")

            // sequence.forEach {  }  // <- iterating that sequence second time will fail
        }

        @Sample
        fun generateSequenceWithSeed() {

            fun fibonacci(): Sequence<Int> {
                // fibonacci terms
                // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
                return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first }
            }

            assertPrints(fibonacci().take(10).toList(), "[0, 1, 1, 2, 3, 5, 8, 13, 21, 34]")
        }

        @Sample
        fun generateSequenceWithLazySeed() {
            class LinkedValue<T>(val value: T, val next: LinkedValue<T>? = null)

            fun <T> LinkedValue<T>?.asSequence(): Sequence<LinkedValue<T>> = generateSequence(
                seedFunction = { this },
                nextFunction = { it.next }
            )

            fun <T> LinkedValue<T>?.valueSequence(): Sequence<T> = asSequence().map { it.value }

            val singleItem = LinkedValue(42)
            val twoItems = LinkedValue(24, singleItem)

            assertPrints(twoItems.valueSequence().toList(), "[24, 42]")
            assertPrints(singleItem.valueSequence().toList(), "[42]")
            assertPrints(singleItem.next.valueSequence().toList(), "[]")
        }

        @Sample
        fun sequenceOfValues() {
            val sequence = sequenceOf("first", "second", "last")
            sequence.forEach(::println)
        }

        @Sample
        fun sequenceFromCollection() {
            val collection = listOf('a', 'b', 'c')
            val sequence = collection.asSequence()

            assertPrints(sequence.joinToString(), "a, b, c")
        }

        @Sample
        fun sequenceFromArray() {
            val array = arrayOf('a', 'b', 'c')
            val sequence = array.asSequence()

            assertPrints(sequence.joinToString(), "a, b, c")
        }

        @Sample
        fun sequenceFromIterator() {
            val array = arrayOf(1, 2, 3)

            // create a sequence with a function, returning an iterator
            val sequence1 = Sequence { array.iterator() }
            assertPrints(sequence1.joinToString(), "1, 2, 3")
            assertPrints(sequence1.drop(1).joinToString(), "2, 3")

            // create a sequence from an existing iterator
            // can be iterated only once
            val sequence2 = array.iterator().asSequence()
            assertPrints(sequence2.joinToString(), "1, 2, 3")
            // sequence2.drop(1).joinToString() // <- iterating sequence second time will fail
        }

        @Sample
        fun sequenceFromEnumeration() {
            val numbers = java.util.Hashtable<String, Int>()
            numbers.put("one", 1)
            numbers.put("two", 2)
            numbers.put("three", 3)

            // when you have an Enumeration from some old code
            val enumeration: java.util.Enumeration<String> = numbers.keys()

            // you can wrap it in a sequence and transform further with sequence operations
            val sequence = enumeration.asSequence().sorted()
            assertPrints(sequence.toList(), "[one, three, two]")

            // the resulting sequence is one-shot
            assertFails { sequence.toList() }
        }

        @Sample
        fun buildFibonacciSequence() {
            fun fibonacci() = buildSequence {
                var terms = Pair(0, 1)

                // this sequence is infinite
                while (true) {
                    yield(terms.first)
                    terms = Pair(terms.second, terms.first + terms.second)
                }
            }

            assertPrints(fibonacci().take(10).toList(), "[0, 1, 1, 2, 3, 5, 8, 13, 21, 34]")
        }

        @Sample
        fun buildSequenceYieldAll() {
            val sequence = buildSequence {
                val start = 0
                // yielding a single value
                yield(start)
                // yielding an iterable
                yieldAll(1..5 step 2)
                // yielding an infinite sequence
                yieldAll(generateSequence(8) { it * 3 })
            }

            assertPrints(sequence.take(7).toList(), "[0, 1, 3, 5, 8, 24, 72]")
        }

        @Sample
        fun buildIterator() {
            val collection = listOf(1, 2, 3)
            val wrappedCollection = object : AbstractCollection<Any>() {
                override val size: Int = collection.size + 2

                override fun iterator(): Iterator<Any> = buildIterator {
                    yield("first")
                    yieldAll(collection)
                    yield("last")
                }
            }

            assertPrints(wrappedCollection, "[first, 1, 2, 3, last]")
        }

    }

    class Usage {

        @Sample
        fun sequenceOrEmpty() {
            val nullSequence: Sequence<Int>? = null
            assertPrints(nullSequence.orEmpty().toList(), "[]")

            val sequence: Sequence<Int>? = sequenceOf(1, 2, 3)
            assertPrints(sequence.orEmpty().toList(), "[1, 2, 3]")
        }

        @Sample
        fun sequenceIfEmpty() {
            val empty = emptySequence<Int>()

            val emptyOrDefault = empty.ifEmpty { sequenceOf("default") }
            assertPrints(emptyOrDefault.toList(), "[default]")

            val nonEmpty = sequenceOf("value")

            val nonEmptyOrDefault = nonEmpty.ifEmpty { sequenceOf("default") }
            assertPrints(nonEmptyOrDefault.toList(), "[value]")
        }
    }

    class Transformations {

        @Sample
        fun takeWindows() {
            val sequence = generateSequence(1) { it + 1 }

            val windows = sequence.windowed(size = 5, step = 1)
            assertPrints(windows.take(4).toList(), "[[1, 2, 3, 4, 5], [2, 3, 4, 5, 6], [3, 4, 5, 6, 7], [4, 5, 6, 7, 8]]")

            val moreSparseWindows = sequence.windowed(size = 5, step = 3)
            assertPrints(moreSparseWindows.take(4).toList(), "[[1, 2, 3, 4, 5], [4, 5, 6, 7, 8], [7, 8, 9, 10, 11], [10, 11, 12, 13, 14]]")

            val fullWindows = sequence.take(10).windowed(size = 5, step = 3)
            assertPrints(fullWindows.toList(), "[[1, 2, 3, 4, 5], [4, 5, 6, 7, 8]]")

            val partialWindows = sequence.take(10).windowed(size = 5, step = 3, partialWindows = true)
            assertPrints(partialWindows.toList(), "[[1, 2, 3, 4, 5], [4, 5, 6, 7, 8], [7, 8, 9, 10], [10]]")
        }

        @Sample
        fun averageWindows() {
            val dataPoints = sequenceOf(10, 15, 18, 25, 19, 21, 14, 8, 5)

            val averaged = dataPoints.windowed(size = 4, step = 1, partialWindows = true) { window -> window.average() }
            assertPrints(averaged.toList(), "[17.0, 19.25, 20.75, 19.75, 15.5, 12.0, 9.0, 6.5, 5.0]")

            val averagedNoPartialWindows = dataPoints.windowed(size = 4, step = 1).map { it.average() }
            assertPrints(averagedNoPartialWindows.toList(), "[17.0, 19.25, 20.75, 19.75, 15.5, 12.0]")
        }

        @Sample
        fun zip() {
            val sequenceA = ('a'..'z').asSequence()
            val sequenceB = generateSequence(1) { it * 2 + 1 }

            assertPrints((sequenceA zip sequenceB).take(4).toList(), "[(a, 1), (b, 3), (c, 7), (d, 15)]")
        }

        @Sample
        fun zipWithTransform() {
            val sequenceA = ('a'..'z').asSequence()
            val sequenceB = generateSequence(1) { it * 2 + 1 }

            val result = sequenceA.zip(sequenceB) { a, b -> "$a/$b" }
            assertPrints(result.take(4).toList(), "[a/1, b/3, c/7, d/15]")
        }
    }

}