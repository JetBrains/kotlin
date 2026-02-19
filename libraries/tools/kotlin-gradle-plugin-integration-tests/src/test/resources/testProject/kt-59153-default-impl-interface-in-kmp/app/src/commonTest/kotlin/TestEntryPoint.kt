import kotlin.test.Test
import kotlin.test.assertEquals

class AppCommonTest {

    @Test
    fun testUsageOfAppInterface() {
        assertEquals(objectADelegate.test { "Hello!" }, "Hello!")
        assertEquals(objectLibDelegate.test { "World!" }, "World!")
    }
}