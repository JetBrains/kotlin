package test.text

import kotlin.test.*
import org.junit.Test

class StringBuilderTest {

    @Test fun stringBuild() {
        val s = buildString {
            append("a")
            append(true)
        }
        assertEquals("atrue", s)
    }

    @Test fun appendMany() {
        assertEquals("a1", StringBuilder().append("a", "1").toString())
        assertEquals("a1", StringBuilder().append("a", 1).toString())
        assertEquals("a1", StringBuilder().append("a", StringBuilder().append("1")).toString())
    }

    @Test fun append() {
        // this test is needed for JS implementation
        assertEquals("em", buildString {
            append("element", 2, 4)
        })
    }

    @Test fun asCharSequence() {
        val original = "Some test string"
        val sb = StringBuilder(original)
        val result = sb.toString()
        val cs = sb as CharSequence

        assertEquals(result.length, cs.length)
        assertEquals(result.length, sb.length)
        for (index in result.indices) {
            assertEquals(result[index], sb[index])
            assertEquals(result[index], cs[index])
        }
        assertEquals(result.substring(2, 6), cs.subSequence(2, 6).toString())
    }

    @Test fun constructors() {
        StringBuilder().let { sb ->
            assertEquals(0, sb.length)
            assertEquals("", sb.toString())
        }

        StringBuilder(16).let { sb ->
            assertEquals(0, sb.length)
            assertEquals("", sb.toString())
        }

        StringBuilder("content").let { sb ->
            assertEquals(7, sb.length)
            assertEquals("content", sb.toString())
        }

        StringBuilder(StringBuilder("content")).let { sb ->
            assertEquals(7, sb.length)
            assertEquals("content", sb.toString())
        }
    }
}
