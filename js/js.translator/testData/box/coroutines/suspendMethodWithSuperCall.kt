// WITH_STDLIB
// EXPECTED_REACHABLE_NODES: 1292

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = ""

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

abstract class Parent {
    val o = "O"
    val k = "K"
    protected fun getO() = o
    protected fun getK() = k
}


class Child : Parent() {
    suspend fun justSomeSuspendFunction() {
        Unit
    }

    suspend fun runTest() {
        justSomeSuspendFunction()
        result += super.getO() + super.getK()
    }
}

fun box(): String {
    builder {
        Child().runTest()
    }
    return result
}
