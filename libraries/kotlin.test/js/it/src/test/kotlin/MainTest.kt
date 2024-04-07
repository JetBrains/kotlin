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

    var expectedAfterLog = ""

    @BeforeTest
    fun before() {
        log = ""
        afterLog = ""
        expectedAfterLog = ""
    }

    @AfterTest
    fun after() {
        assertEquals(afterLog, expectedAfterLog)
    }

    fun promise(v: Int, after: String = "") = Promise<Int> { resolve, reject ->
        log += "a"
        js("setTimeout")({ log += "c"; afterLog += after; resolve(v) }, 100)
        log += "b"
    }.also {
        expectedAfterLog += after
    }

    @Test
    fun checkAsyncOrder(): Promise<Unit> {
        log += 1

        val p1 = promise(10, "after")

        log += 2

        val p2 = p1.then { result ->
            assertEquals(log, "1ab23c")
        }

        log += 3

        return p2
    }

    @Test
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun checkCustomPromise(): CustomPromise {
        return promise(10, "afterCustom") as CustomPromise
    }

    @Test
    fun asyncPassing() = promise(10).then { assertEquals(10, it) }

    @Test
    fun asyncFailing() = promise(20).then { assertEquals(10, it) }
}

@JsName("Promise")
external class CustomPromise {}