/**
 * @author Sergey Mashkov aka cy6erGn0m
 * @since 28.07.12
 */

import junit.framework.TestCase
import kotlin.regexp.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegexpTest() : TestCase() {
    val p = "[0-9]+".toRegex()

    fun testIterator() {
        val rs = arrayList<String?>()

        for (m in "1 22 333 4444 55555".createMatcher(p)) {
            rs.add(m.group())
        }

        assertEquals(arrayList("1", "22", "333", "4444", "55555"), rs)
    }

    fun testForEachMatchResult() {
        val rs = arrayList<String?>()

        "1 22 333 4444 55555".createMatcher(p).forEachMatchResult { m->
            rs.add(m.group())
        }

        assertEquals(arrayList("1", "22", "333", "4444", "55555"), rs)
    }

    fun testForEachMatch() {
        val rs = arrayList<String?>()

        "1 22 333 4444 55555".createMatcher(p).forEachMatch { text->
            rs.add(text)
        }

        assertEquals(arrayList("1", "22", "333", "4444", "55555"), rs)
    }

    fun testAllMatchResults() {
        val rs = "1 22 333 4444 55555".createMatcher(p).allMatchResults().map {it.group()}
        assertEquals(arrayList("1", "22", "333", "4444", "55555"), rs)
    }

    fun testAllMatches() {
        val rs = "1 22 333 4444 55555".createMatcher(p).allMatches()
        assertEquals(arrayList("1", "22", "333", "4444", "55555"), rs)
    }

    fun testReplace() {
        assertEquals("before 1 2 3 4 5 after", "before 1 22 333 4444 55555 after".createMatcher(p).replace {
            it.group()!!.charAt(0).toString()
        })
    }
}

