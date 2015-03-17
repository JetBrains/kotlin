package iterators

import kotlin.test.assertEquals
import org.junit.Test as test
import kotlin.test.fails
import java.util.ArrayList

fun fibonacci(): Sequence<Int> {
    // fibonacci terms
    var index = 0; var a = 0; var b = 1
    return sequence<Int> { when (index++) { 0 -> a; 1 -> b; else -> { val result = a + b; a = b; b = result; result } } }
}

class IteratorsTest {

    test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(arrayListOf(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    test fun foldReducesTheFirstNElements() {
        val sum = { (a: Int, b: Int) -> a + b }
        assertEquals(arrayListOf(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    test fun takeExtractsTheFirstNElements() {
        assertEquals(arrayListOf(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(arrayListOf(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { (i: Int) -> i < 20 }.toList())
    }

    test fun mapIndexed() {
        assertEquals(arrayListOf(0, 1, 2, 6, 12), fibonacci().mapIndexed { index, value -> index * value }.takeWhile {(i: Int) -> i < 20 }.toList())
    }

    test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.joinToString(separator = ", ", limit = 5))
    }

    test fun plus() {
        val iter = arrayListOf("foo", "bar").sequence()
        val iter2 = iter + "cheese"
        assertEquals(arrayListOf("foo", "bar", "cheese"), iter2.toList())

        // lets use a mutable variable
        var mi  = sequenceOf("a", "b")
        mi += "c"
        assertEquals(arrayListOf("a", "b", "c"), mi.toList())
    }

    test fun plusCollection() {
        val a = arrayListOf("foo", "bar")
        val b = arrayListOf("cheese", "wine")
        val iter = a.sequence() + b.sequence()
        assertEquals(arrayListOf("foo", "bar", "cheese", "wine"), iter.toList())

        // lets use a mutable variable
        var ml = arrayListOf("a").sequence()
        ml += a.sequence()
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(arrayListOf("a", "foo", "bar", "beer", "cheese", "wine", "z"), ml.toList())
    }

    test fun requireNoNulls() {
        val iter = arrayListOf<String?>("foo", "bar").sequence()
        val notNull = iter.requireNoNulls()
        assertEquals(arrayListOf("foo", "bar"), notNull.toList())

        val iterWithNulls = arrayListOf("foo", null, "bar").sequence()
        val notNull2 = iterWithNulls.requireNoNulls()
        fails {
            // should throw an exception as we have a null
            notNull2.toList()
        }
    }

    test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter {  it > 10 }.joinToString(limit = 10))
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.joinToString())
    }

    test fun pairIterator() {
        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).joinToString(limit = 10)
        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
    }

    test fun skippingIterator() {
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(7).joinToString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().drop(3).drop(4).joinToString(limit = 10))
    }

    test fun iterationOverIterator() {
        val c = arrayListOf(0, 1, 2, 3, 4, 5)
        var s = ""
        for (i in c.iterator()) {
            s = s + i.toString()
        }
        assertEquals("012345", s)
    }

    private fun <T, C : MutableCollection<in T>> Iterator<T>.takeWhileTo(result: C, predicate: (T) -> Boolean): C {
        for (element in this) if (predicate(element)) result.add(element) else break
        return result
    }

    test fun iterableExtension() {
        val c = arrayListOf(0, 1, 2, 3, 4, 5)
        val d = ArrayList<Int>()
        c.iterator().takeWhileTo(d, {i -> i < 4 })
        assertEquals(4, d.size())
    }
}
