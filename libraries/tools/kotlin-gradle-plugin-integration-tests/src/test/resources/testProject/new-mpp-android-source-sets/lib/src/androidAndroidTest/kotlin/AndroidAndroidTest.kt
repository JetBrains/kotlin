import kotlin.test.Test

/**
 * Expected to fail!
 */
class AndroidAndroidTest {
    @Test
    fun fail() {
        MainApiKotlin.sayHi()
        MainApiJava.sayHi()
        CommonApi.throwException()
    }
}