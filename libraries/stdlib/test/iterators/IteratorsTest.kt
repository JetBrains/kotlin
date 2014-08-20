package iterators

import kotlin.test.assertEquals
import org.junit.Test as test
import kotlin.test.fails
import java.util.ArrayList

fun fibonacci(): Iterator<Int> {
    // fibonacci terms
    var index = 0; var a = 0; var b = 1
    return iterate<Int> { when (index++) { 0 -> a; 1 -> b; else -> { val result = a + b; a = b; b = result; result } } }
}

class IteratorsTest {

    test fun filterAndTakeWhileExtractTheElementsWithinRange() {
        assertEquals(arrayList(144, 233, 377, 610, 987), fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.toList())
    }

    test fun foldReducesTheFirstNElements() {
        val sum = { (a: Int, b: Int) -> a + b }
        assertEquals(arrayList(13, 21, 34, 55, 89).fold(0, sum), fibonacci().filter { it > 10 }.take(5).fold(0, sum))
    }

    test fun takeExtractsTheFirstNElements() {
        assertEquals(arrayList(0, 1, 1, 2, 3, 5, 8, 13, 21, 34), fibonacci().take(10).toList())
    }

    test fun mapAndTakeWhileExtractTheTransformedElements() {
        assertEquals(arrayList(0, 3, 3, 6, 9, 15), fibonacci().map { it * 3 }.takeWhile { (i: Int) -> i < 20 }.toList())
    }

    test fun joinConcatenatesTheFirstNElementsAboveAThreshold() {
        assertEquals("13, 21, 34, 55, 89, ...", fibonacci().filter { it > 10 }.makeString(separator = ", ", limit = 5))
    }

    test fun plus() {
        val iter = arrayList("foo", "bar").iterator()
        val iter2 = iter + "cheese"
        assertEquals(arrayList("foo", "bar", "cheese"), iter2.toList())

        // lets use a mutable variable
        var mi : Iterator<String> = arrayList("a", "b").iterator()
        mi += "c"
        assertEquals(arrayList("a", "b", "c"), mi.toList())
    }

    test fun plusCollection() {
        val a = arrayList("foo", "bar")
        val b = arrayList("cheese", "wine")
        val iter = a.iterator() + b.iterator()
        assertEquals(arrayList("foo", "bar", "cheese", "wine"), iter.toList())

        // lets use a mutable variable
        var ml : Iterator<String> = arrayList("a").iterator()
        ml += a.iterator()
        ml += "beer"
        ml += b
        ml += "z"
        assertEquals(arrayList("a", "foo", "bar", "beer", "cheese", "wine", "z"), ml.toList())
    }

    test fun requireNoNulls() {
        val iter = arrayList<String?>("foo", "bar").iterator()
        val notNull = iter.requireNoNulls()
        assertEquals(arrayList("foo", "bar"), notNull.toList())

        val iterWithNulls = arrayList("foo", null, "bar").iterator()
        val notNull2 = iterWithNulls.requireNoNulls()
        fails {
            // should throw an exception as we have a null
            notNull2.toList()
        }
    }

    test fun toStringJoinsNoMoreThanTheFirstTenElements() {
        assertEquals("0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...", fibonacci().makeString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().filter {  it > 10 }.makeString(limit = 10))
        assertEquals("144, 233, 377, 610, 987", fibonacci().filter { it > 100 }.takeWhile { it < 1000 }.makeString())
    }

    test fun pairIterator() {
        val pairStr = (fibonacci() zip fibonacci().map { i -> i*2 }).makeString(limit = 10)
        assertEquals("(0, 0), (1, 2), (1, 2), (2, 4), (3, 6), (5, 10), (8, 16), (13, 26), (21, 42), (34, 68), ...", pairStr)
    }

    test fun skippingIterator() {
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().skip(7).makeString(limit = 10))
        assertEquals("13, 21, 34, 55, 89, 144, 233, 377, 610, 987, ...", fibonacci().skip(3).skip(4).makeString(limit = 10))
    }

    test fun iterationOverIterator() {
        val c = arrayList(0, 1, 2, 3, 4, 5)
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
        val c = arrayList(0, 1, 2, 3, 4, 5)
        val d = ArrayList<Int>()
        c.iterator().takeWhileTo(d, {i -> i < 4 })
        assertEquals(4, d.size())
    }
}
