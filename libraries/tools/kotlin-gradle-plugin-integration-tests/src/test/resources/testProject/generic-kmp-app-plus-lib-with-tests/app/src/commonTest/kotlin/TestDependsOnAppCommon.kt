import kotlin.test.Test
import kotlin.test.assertEquals

class AppCommonTest {

    @Test
    fun sayYesSaysYes() {
        assertEquals("Yes", appCommonFunForAppPlatformAndAppCommonTest())
    }
}