import org.junit.Test

class AndroidTestKotlin {
    @Test
    fun fail() {
        MainApiJava.sayHi()
        MainApiKotlin.sayHi()
        CommonApi.throwException()
    }
}