package samples.collections

import samples.*
import kotlin.test.*
import kotlin.coroutines.experimental.buildIterator
import kotlin.coroutines.experimental.buildSequence

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
        fun buildFibonacciSequence() {
            fun fibonacci() = buildSequence {
                var terms = Pair(0, 1)

                // this sequence is infinite
                while(true) {
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


}