import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancedTest {

    @Test
    fun jvmUtilReturns400() {
        assertEquals(400, libJvmPlatformUtil())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, multiplyByTwo(1))
    }
}