import org.junit.Test

class TestJava {

    @Test
    fun fail() {
        MainApiKotlin.sayHi()
        MainApiJava.sayHi()
        AndroidMainApiKotlin.sayHi()
        CommonApi.throwException()
    }

}