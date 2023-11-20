import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancedTest {

    @Test
    fun jsUtilReturns0() {
        assertEquals(0, libJsPlatformUtil())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, multiplyByTwo(1))
    }
}