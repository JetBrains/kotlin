package test.js

import kotlin.js.*
import kotlin.test.*

@JsFun("async () => 'foo'")
internal external fun jsAsyncFoo(): UntypedPromise

@JsFun("() => 'foo'")
internal external fun jsFoo(): JsValue

var state: JsValue? = null

class MyThrowable : Throwable()

class AsyncTest {
    @Test
    fun test1(): UntypedPromise {
        var thenExecuted = false
        val p = jsAsyncFoo().then { value ->
            state = value
            thenExecuted = true
            null
        }
        assertFalse(thenExecuted)
        return p
    }

    @Test
    fun test2(): UntypedPromise {
        assertEquals(state, jsFoo())
        var thenExecuted = false
        val p = jsAsyncFoo().then {
            assertEquals(state, jsFoo())
            thenExecuted = true
            null
        }.then {
            assertEquals(state, jsFoo())
            thenExecuted = true
            null
        }
        assertFalse(thenExecuted)
        return p
    }

    @Test
    fun testJsValueToThrowableOrNull1(): UntypedPromise {
        val p = jsAsyncFoo().then {
            throw MyThrowable()
            null
        }.catch { e ->
            assert(e.toThrowableOrNull() is MyThrowable)
            null
        }
        return p
    }

    @Test
    fun testJsValueToThrowableOrNull2() {
        val e = MyThrowable()
        assertEquals((e as JsValue).toThrowableOrNull(), e)
    }
}