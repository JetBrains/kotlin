// ISSUE: KT-70078

// FILE: ComplexOptions.kt
@file:JsQualifier("foo.bar")

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface ComplexOptions {
    val name: String
    val method: String
}

// FILE: main.kt

import kotlinx.js.JsPlainObject
import kotlin.coroutines.*

private object EmptyContinuation: Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

@JsPlainObject
external interface Options {
    val method: String
}

suspend fun getMethod(): String = "GET"

fun box(): String {
    var result = "OK"

    builder {
        val options1 = Options(method = getMethod())
        if (options1.method != "GET") {
            result = options1.method
        }
        val method = getMethod()
        val options2 = Options(method = method)
        if (options2.method != "GET") {
            result = options2.method
        }
        val complexOptions = ComplexOptions(method = getMethod(), name = "test")
        if (complexOptions.method != "GET") {
            result = complexOptions.method
        }
        val complexOptions2 = ComplexOptions(method = getMethod(), name = "test")
        if (complexOptions2.method != "GET") {
            result = complexOptions2.method
        }
    }

    return result
}
