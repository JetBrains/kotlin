import kotlin.test.Test
import kotlin.test.assertEquals

class AdvancedTest {

    @Test
    fun nativeUtilReturns800() {
        assertEquals(800, libNativePlatformUtil())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, multiplyByTwo(1))
    }
}
