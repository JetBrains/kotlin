package test.collections

import org.junit.Test as test
import kotlin.test.*

class NaturalLanguageTest {

    @test fun `strings equal`() {
        val actual = "abc"
        assertEquals("abc", actual)
    }

    @test fun `numbers equal`() {
        val actual = 5
        assertEquals(5, actual)
    }
}