import kotlin.test.Test

/* Expected to fail ! */
class CommonTest {
    @Test
    fun fail() {
        CommonApi.throwException()
    }
}