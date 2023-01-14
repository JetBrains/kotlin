package test.js

import kotlin.js.*
import kotlin.test.*

internal fun jsAsyncFoo(): Promise<Dynamic?> =
    js("(async () => 'foo')()")

internal fun jsFoo(): Dynamic =
    js("'foo'")

var state: Dynamic? = null

class MyThrowable : Throwable()

class AsyncTest {
    @Test
    fun test1(): Promise<Dynamic?> {
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
    fun test2(): Promise<Dynamic?> {
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
    fun testJsValueToThrowableOrNull1(): Promise<Dynamic?> {
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
        assertEquals((e as Dynamic).toThrowableOrNull(), e)
    }
}