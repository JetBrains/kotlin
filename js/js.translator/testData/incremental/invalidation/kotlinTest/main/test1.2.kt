import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

class Test1 {
    @BeforeTest
    fun before() {
        call("before")
    }

    @AfterTest
    fun after() {
        call("after")
    }

    @Test
    fun foo() {
        call("foo")
    }

    @Test
    fun withException() {
        call("withException")
        raise("some exception")
        call("never happens")
    }
}
