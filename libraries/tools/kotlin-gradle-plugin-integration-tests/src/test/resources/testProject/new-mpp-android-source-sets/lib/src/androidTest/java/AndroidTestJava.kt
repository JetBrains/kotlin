import org.junit.Test

class AndroidTestJava {
    @Test
    fun fail() {
        MainApiJava.sayHi()
        MainApiKotlin.sayHi()
        CommonApi.throwException()
    }
}