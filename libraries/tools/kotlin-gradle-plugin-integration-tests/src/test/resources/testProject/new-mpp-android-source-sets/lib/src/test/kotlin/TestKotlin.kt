import kotlin.test.Test

/**
 * Expected to fail!
 */
class TestKotlin {
    @Test
    fun fail() {
        MainApiKotlin.sayHi()
        MainApiJava.sayHi()
        CommonApi.throwException()
    }
}