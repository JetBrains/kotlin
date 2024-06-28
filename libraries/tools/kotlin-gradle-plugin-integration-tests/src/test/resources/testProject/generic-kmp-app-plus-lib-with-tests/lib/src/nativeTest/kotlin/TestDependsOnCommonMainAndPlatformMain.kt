import kotlin.test.Test
import kotlin.test.assertEquals

class LibNativeTest {

    @Test
    fun nativeUtilReturns800() {
        assertEquals(800, libNativePlatformUtil().toInt())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, libCommonFunForLibPlatformTests(1))
    }
}
