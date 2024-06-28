import kotlin.test.Test
import kotlin.test.assertEquals

class LibJvmTest {

    @Test
    fun jvmUtilReturns400() {
        assertEquals(400, libJvmPlatformUtil().toInt())
    }

    @Test
    fun commonUtilTest() {
        assertEquals(2, libCommonFunForLibPlatformTests(1))
    }
}