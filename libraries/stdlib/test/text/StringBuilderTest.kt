package test.text

import kotlin.test.*
import org.junit.Test as test

class StringBuilderTest {

    @test fun stringBuild() {
        val s = buildString {
            append("a")
            append(true)
        }
        assertEquals("atrue", s)
    }

    @test fun appendMany() {
        assertEquals("a1", StringBuilder().append("a", "1").toString())
        assertEquals("a1", StringBuilder().append("a", 1).toString())
        assertEquals("a1", StringBuilder().append("a", StringBuilder().append("1")).toString())
    }

    @test fun append() {
        // this test is needed for JS implementation
        assertEquals("em", buildString {
            append("element", 2, 4)
        })
    }
}
