import kotlin.test.Test
import kotlin.test.assertEquals

class SomeTest {

    @Test
    fun <caret>testSomething() {
        assertEquals(4, 2 + 2)
    }
}