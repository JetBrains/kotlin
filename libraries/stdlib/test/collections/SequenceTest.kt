package test.collections

import org.junit.Test
import kotlin.test.*
import kotlin.comparisons.*

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    // 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, ...
    return generateSequence(Pair(0, 1), { Pair(it.second, it.first + it.second) }).map { it.first * 1 }
}

public class SequenceTest {

    @Test fun filterEmptySequence() {
        for (sequence in listOf(emptySequence<String>(), sequenceOf<String>())) {
            assertEquals(0, sequence.filter { false }.count())
            assertEquals(0, sequence.filter { true }.count())
        }
    }

    @Test fun mapEmptySequence() {
        for (sequence in listOf(emptySequence<String>(), sequenceOf<String>())) {
            assertEquals(0, sequence.map { true }.count())
        }
    }

    @Test fun requireNoNulls() {
        val sequence = sequenceOf<String?>("foo", "bar")
        val notNull = sequence.requireNoNulls()
        assertEquals(listOf("foo", "bar"), notNull.toList())

        val sequenceWithNulls = sequenceOf("foo", null, "bar")
        val notNull2 = sequenceWithNulls.requireNoNulls() // shouldn't fail yet
        assertFails {
            // should throw an exception as we have a null
            notNull2.toList()
        }
    }

    @Test fun filterIndexed() {
        assertEquals(listOf(1, 2, 5, 13, 34), fibonacci().filterIndexed { index, _ -> index % 2 == 1 }.take(5).toList())
    }

    @Test fun filterNullable() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filter { it == null || it == "foo" }
        assertEquals(listOf(null, "foo", null), filtered.toList())
    }

    @Test fun filterNot() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filterNot { it == null }
        assertEquals(listOf("foo", "bar"), filtered.toList())
    }

    @Test fun filterNotNull() {
        val data = sequenceOf(null, "foo", null, "bar")
        val filtered = data.filterNotNull()
        assertEquals(listOf("foo", "bar"), filtered.toList())
    }

    @Test fun mapIndexed() {
        assertEquals(listOf(0, 1, 2, 6, 12), fibonacci().mapIndexed { index, value -> index * value }.takeWhile { i: Int -> i < 20 }.toList())
    }

    @Test fun mapNotNull() {
        assertEquals(listOf(0, 10, 110, 1220), fibonacci().mapNotNull { if (it % 5 == 0) it * 2 else null }.take(4).toList())
    }

    @Test fun mapIndexedNotNull() {
        // find which terms are divisible by their index
        assertEquals(listOf("1/1", "5/5", "144/12", "46368/24", "75025/25"),
                fibonacci().mapIndexedNotNull { index, value ->
                    if (index > 0 && (value % index) == 0) "$value/$index" else null
                }.take(5).toList())
    }


    @Test fun mapAndJoinToString() {
        assertEquals("3, 5, 8", fibonacci().withIndex().filter { it.index > 3 }.take(3).joinToString { it.value.toString() })
    }

    @Test fun withIndex() {
        val data = sequenceOf("foo", "bar")
        val indexed = data.withIndex().map { it.value.substring(0..it.index) }.toList()
        assertEquals(listOf("f", "ba"), indexed)
    }

    @Test
    fun onEach() {
        var count = 0
        val data = sequenceOf("foo", "bar")
        val newData = data.onEach { count += it.length }
        assertFalse(data === newData)
        assertEquals(0, count, "onEach should be executed lazily")

        data.forEach {  }
        assertEquals(0, count, "onEach should be executed only when resulting sequence is iterated")

        val sum = newData.sumBy { it.length }
        assertEquals(sum, count)
    }


    @Test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(listOf(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    @Test fun foldReducesTheFirstNElements() {
        val sum = { a: Int, b: Int -> a + b }
        assertEquals(listOf(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    @Test fun takeExtractsTheFirstNElements() {
        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    @Test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(listOf(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { i: Int -> i < 20 }.toList())
    }

    @Test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.joinToString(separator = ", ", limit = 5))
    }

    @Test fun drop() {
        assertEquals(emptyList(), emptySequence<Int>().drop(1).toList())
        listOf(2, 3, 4, 5).let { assertEquals(it, it.asSequence().drop(0).toList()) }
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(7).joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(3).drop(4).joinToString(limit = 10))
        assertFailsWith<IllegalArgumentException> { fibonacci().drop(-1) }
    }

    @Test fun take() {
        assertEquals(emptyList(), emptySequence<Int>().take(1).toList())
        assertEquals(emptyList(), fibonacci().take(0).toList())

        assertEquals("0, 1, 1, 2, 3, 5, 8", fibonacci().take(7).joinToString())
        assertEquals("0, 1, 1, 2", fibonacci().take(7).take(4).joinToString())
        assertEquals("0, 1, 1, 2", fibonacci().take(4).take(5).joinToString())

        assertEquals(emptyList(), fibonacci().take(1).drop(1).toList())
        assertEquals(emptyList(), fibonacci().take(1).drop(2).toList())

        assertFailsWith<IllegalArgumentException> { fibonacci().take(-1) }
    }

    @Test fun subSequence() {
        assertEquals(listOf(2, 3, 5, 8), fibonacci().drop(3).take(4).toList())
        assertEquals(listOf(2, 3, 5, 8), fibonacci().take(7).drop(3).toList())

        val seq = fibonacci().drop(3).take(4)

        assertEquals(listOf(2, 3, 5, 8), seq.take(5).toList())
        assertEquals(listOf(2, 3, 5), seq.take(3).toList())

        assertEquals(emptyList(), seq.drop(5).toList())
        assertEquals(listOf(8), seq.drop(3).toList())

    }

    @Test fun dropWhile() {
        assertEquals("233, 377, 610", fibonacci().dropWhile { it < 200 }.take(3).joinToString(limit = 10))
        assertEquals("", sequenceOf(1).dropWhile { it < 200 }.joinToString(limit = 10))
    }

    @Test fun zip() {
        expect(listOf("ab", "bc", "cd")) {
            sequenceOf("a", "b", "c").zip(sequenceOf("b", "c", "d")) { a, b -> a + b }.toList()
        }
    }

//    @Test fun zipPairs() {
//        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).joinToString(limit = 10)
//        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
//    }

    @Test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter { it > 10 }.joinToString(limit = 10))
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.joinToString())
    }


    fun testPlus(doPlus: (Sequence<String>) -> Sequence<String>) {
        val seq = sequenceOf("foo", "bar")
        val seq2: Sequence<String> = doPlus(seq)
        assertEquals(listOf("foo", "bar"), seq.toList())
        assertEquals(listOf("foo", "bar", "cheese", "wine"), seq2.toList())
    }


    @Test fun plusElement() = testPlus { it + "cheese" + "wine" }
    @Test fun plusCollection() = testPlus { it + listOf("cheese", "wine") }
    @Test fun plusArray() = testPlus { it + arrayOf("cheese", "wine") }
    @Test fun plusSequence() = testPlus { it + sequenceOf("cheese", "wine") }

    @Test fun plusAssign() {
        // lets use a mutable variable
        var seq = sequenceOf("a")
        seq += "foo"
        seq += listOf("beer")
        seq += arrayOf("cheese", "wine")
        seq += sequenceOf("bar", "foo")
        assertEquals(listOf("a", "foo", "beer", "cheese", "wine", "bar", "foo"), seq.toList())
    }

    private fun testMinus(expected: List<String>? = null, doMinus: (Sequence<String>) -> Sequence<String>) {
        val a = sequenceOf("foo", "bar", "bar")
        val b: Sequence<String> = doMinus(a)
        val expected_ = expected ?: listOf("foo")
        assertEquals(expected_, b.toList())
    }

    @Test fun minusElement() = testMinus(expected = listOf("foo", "bar")) { it - "bar" - "zoo" }
    @Test fun minusCollection() = testMinus { it - listOf("bar", "zoo") }
    @Test fun minusArray() = testMinus { it - arrayOf("bar", "zoo") }
    @Test fun minusSequence() = testMinus { it - sequenceOf("bar", "zoo") }

    @Test fun minusIsLazyIterated() {
        val seq = sequenceOf("foo", "bar")
        val list = arrayListOf<String>()
        val result = seq - list

        list += "foo"
        assertEquals(listOf("bar"), result.toList())
        list += "bar"
        assertEquals(emptyList<String>(), result.toList())
    }

    @Test fun minusAssign() {
        // lets use a mutable variable of readonly list
        val data = sequenceOf("cheese", "foo", "beer", "cheese", "wine")
        var l = data
        l -= "cheese"
        assertEquals(listOf("foo", "beer", "cheese", "wine"), l.toList())
        l = data
        l -= listOf("cheese", "beer")
        assertEquals(listOf("foo", "wine"), l.toList())
        l -= arrayOf("wine", "bar")
        assertEquals(listOf("foo"), l.toList())
    }



    @Test fun iterationOverSequence() {
        var s = ""
        for (i in sequenceOf(0, 1, 2, 3, 4, 5)) {
            s += i.toString()
        }
        assertEquals("012345", s)
    }

    @Test fun sequenceFromFunction() {
        var count = 3

        val sequence = generateSequence {
            count--
            if (count >= 0) count else null
        }

        val list = sequence.toList()
        assertEquals(listOf(2, 1, 0), list)

        assertFails {
            sequence.toList()
        }
    }

    @Test fun sequenceFromFunctionWithInitialValue() {
        val values = generateSequence(3) { n -> if (n > 0) n - 1 else null }
        val expected = listOf(3, 2, 1, 0)
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "Iterating sequence second time yields the same result")
    }

    @Test fun sequenceFromFunctionWithLazyInitialValue() {
        var start = 3
        val values = generateSequence({ start }, { n -> if (n > 0) n - 1 else null })
        val expected = listOf(3, 2, 1, 0)
        assertEquals(expected, values.toList())
        assertEquals(expected, values.toList(), "Iterating sequence second time yields the same result")

        start = 2
        assertEquals(expected.drop(1), values.toList(), "Initial value function is called on each iterator request")

        // does not throw on construction
        val errorValues = generateSequence<Int>({ (throw IllegalStateException()) }, { null })
        // does not throw on iteration
        val iterator = errorValues.iterator()
        // throws on advancing
        assertFails { iterator.next() }
    }


    @Test fun sequenceFromIterator() {
        val list = listOf(3, 2, 1, 0)
        val iterator = list.iterator()
        val sequence = iterator.asSequence()
        assertEquals(list, sequence.toList())
        assertFails {
            sequence.toList()
        }
    }

    @Test fun makeSequenceOneTimeConstrained() {
        val sequence = sequenceOf(1, 2, 3, 4)
        sequence.toList()
        sequence.toList()

        val oneTime = sequence.constrainOnce()
        oneTime.toList()
        assertTrue("should fail with IllegalStateException") {
            assertFails {
                oneTime.toList()
            } is IllegalStateException
        }

    }

    private fun <T, C : MutableCollection<in T>> Sequence<T>.takeWhileTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element) else break
        return result
    }

    @Test fun sequenceExtensions() {
        val d = ArrayList<Int>()
        sequenceOf(0, 1, 2, 3, 4, 5).takeWhileTo(d, { i -> i < 4 })
        assertEquals(4, d.size)
    }

    @Test fun flatMapAndTakeExtractTheTransformedElements() {
        val expected = listOf(
                '3', // fibonacci(4) = 3
                '5', // fibonacci(5) = 5
                '8', // fibonacci(6) = 8
                '1', '3', // fibonacci(7) = 13
                '2', '1', // fibonacci(8) = 21
                '3', '4', // fibonacci(9) = 34
                '5' // fibonacci(10) = 55
                             )

        assertEquals(expected, fibonacci().drop(4).flatMap { it.toString().asSequence() }.take(10).toList())
    }

    @Test fun flatMap() {
        val result = sequenceOf(1, 2).flatMap { (0..it).asSequence() }
        assertEquals(listOf(0, 1, 0, 1, 2), result.toList())
    }

    @Test fun flatMapOnEmpty() {
        val result = sequenceOf<Int>().flatMap { (0..it).asSequence() }
        assertTrue(result.none())
    }

    @Test fun flatMapWithEmptyItems() {
        val result = sequenceOf(1, 2, 4).flatMap { if (it == 2) sequenceOf<Int>() else (it - 1..it).asSequence() }
        assertEquals(listOf(0, 1, 3, 4), result.toList())
    }

    @Test fun flatten() {
        val expected = listOf(0, 1, 0, 1, 2)

        val seq = sequenceOf((0..1).asSequence(), (0..2).asSequence()).flatten()
        assertEquals(expected, seq.toList())

        val seqMappedSeq = sequenceOf(1, 2).map { (0..it).asSequence() }.flatten()
        assertEquals(expected, seqMappedSeq.toList())

        val seqOfIterable = sequenceOf(0..1, 0..2).flatten()
        assertEquals(expected, seqOfIterable.toList())

        val seqMappedIterable = sequenceOf(1, 2).map { 0..it }.flatten()
        assertEquals(expected, seqMappedIterable.toList())
    }

    @Test fun distinct() {
        val sequence = fibonacci().dropWhile { it < 10 }.take(20)
        assertEquals(listOf(1, 2, 3, 0), sequence.map { it % 4 }.distinct().toList())
    }

    @Test fun distinctBy() {
        val sequence = fibonacci().dropWhile { it < 10 }.take(20)
        assertEquals(listOf(13, 34, 55, 144), sequence.distinctBy { it % 4 }.toList())
    }

    @Test fun unzip() {
        val seq = sequenceOf(1 to 'a', 2 to 'b', 3 to 'c')
        val (ints, chars) = seq.unzip()
        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf('a', 'b', 'c'), chars)
    }

    @Test fun sorted() {
        sequenceOf(3, 7, 5).let {
            it.sorted().iterator().assertSorted { a, b -> a <= b }
            it.sortedDescending().iterator().assertSorted { a, b -> a >= b }
        }
    }

    @Test fun sortedBy() {
        sequenceOf("it", "greater", "less").let {
            it.sortedBy { it.length }.iterator().assertSorted { a, b -> compareValuesBy(a, b) { it.length } <= 0 }
            it.sortedByDescending { it.length }.iterator().assertSorted { a, b -> compareValuesBy(a, b) { it.length } >= 0 }
        }

        sequenceOf('a', 'd', 'c', null).let {
            it.sortedBy {it}.iterator().assertSorted { a, b -> compareValues(a, b) <= 0 }
            it.sortedByDescending {it}.iterator().assertSorted { a, b ->  compareValues(a, b) >= 0 }
        }
    }

    @Test fun sortedWith() {
        val comparator = compareBy { s: String -> s.reversed() }
        assertEquals(listOf("act", "wast", "test"), sequenceOf("act", "test", "wast").sortedWith(comparator).toList())
    }

    /*
    test fun pairIterator() {
        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).joinToString(limit = 10)
        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
    }
*/

}