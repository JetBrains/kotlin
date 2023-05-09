// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1431

// MODULE: lib
// FILE: lib.kt
import kotlin.coroutines.*

var resume: () -> Unit = {}

suspend fun suspendAndReturn(msg: String): String {
    return suspendCoroutine<String> { cont ->
        resume = {
            cont.resume(msg)
        }
    }
}

suspend fun <T> runSus(block: suspend () -> T): T = block()

abstract class A {
    fun f() = o()

    abstract fun o(): String
}

// MODULE: libInline(lib)
// FILE: libInline.kt

suspend inline fun foo(): String {
    val a = object : A() {
        override fun o(): String = "O"
    }
    val k = "K"

    return suspendAndReturn(a.f()) + runSus {
        val b = object : A() {
            override fun o(): String = k
        }
        runSus { suspendAndReturn(b.f()) }
    }
}

// MODULE: main(libInline, lib)
// FILE: main.kt

import kotlin.coroutines.*

fun box(): String {
    var testResult: String = "fail"

    val continuation = Continuation<String>(EmptyCoroutineContext) { result ->
        testResult = result.getOrThrow()
    }

    // Test the noinline version of public inline suspend functions
    js("libInline").foo(continuation)
    resume()
    resume()

    return testResult
}
