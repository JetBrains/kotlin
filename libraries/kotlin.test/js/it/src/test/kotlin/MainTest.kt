import kotlin.js.Promise
import kotlin.test.*

var value = 5

class SimpleTest {

    @BeforeTest
    fun beforeFun() {
        value *= 2
    }

    @AfterTest
    fun afterFun() {
        value /= 2
    }

    @Test
    fun testFoo() {
        assertNotEquals(value, foo())
    }

    @Test
    fun testBar() {
        assertEquals(value, foo())
    }

    @Ignore
    @Test
    fun testFooWrong() {
        assertEquals(20, foo())
    }

}

@Ignore
class TestTest {
    @Test
    fun emptyTest() {
    }
}

class AsyncTest {

    var log = ""

    var afterLog = ""

    @BeforeTest
    fun before() {
        log = ""
    }

    // Until bootstrap update
    @AfterTest
    fun after() {
        assertEquals(afterLog, "after")
    }

    fun promise(v: Int) = Promise<Int> { resolve, reject ->
        log += "a"
        js("setTimeout")({ log += "c"; afterLog += "after"; resolve(v) }, 100)
        log += "b"
    }

    @Test
    fun checkAsyncOrder(): Promise<Unit> {
        log += 1

        val p1 = promise(10)

        log += 2

        val p2 = p1.then { result ->
            assertEquals(log, "1ab23c")
        }

        log += 3

        return p2
    }

    @Test
    fun asyncPassing() = promise(10).then { assertEquals(10, it) }

    @Test
    fun asyncFailing() = promise(20).then { assertEquals(10, it) }
}