import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

class Test2 {
    @BeforeTest
    fun before() {
        call("before")
    }

    @Test
    fun foo() {
        call("foo")
    }
}
