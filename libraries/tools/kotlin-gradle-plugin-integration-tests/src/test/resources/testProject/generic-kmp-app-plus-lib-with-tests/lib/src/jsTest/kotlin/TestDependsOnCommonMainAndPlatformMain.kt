import kotlin.test.Test
import kotlin.test.assertEquals

class LibJsTest {

    @Test
    fun jsUtilReturns0() {
        assertEquals(0, libJsPlatformUtil().toInt())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, libCommonFunForLibPlatformTests(1))
    }
}