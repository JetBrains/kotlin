package test

import org.junit.Test as test
import kotlin.test.*

class NaturalLanguageTest {

    test fun `test strings equal`() {
        val actual = "abc"
        assertEquals("abc", actual)
    }

    test fun `test numbers equal`() {
        val actual = 5
        assertEquals(5, actual)
    }
}