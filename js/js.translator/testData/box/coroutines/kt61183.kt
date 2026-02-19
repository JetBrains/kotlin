// KT-61183
// WITH_STDLIB
// WITH_COROUTINES

package foo

import kotlin.coroutines.*

class Exception1() : Exception()
class Exception2() : Exception()

class A {
    suspend fun execute(): Boolean {
        try {
            return true
        } catch (e: Exception1) {
            val result: Boolean = syncFun1()
            if (result) {
                return suspendFun1()
            } else {
                return suspendFun2()
            }
        } catch (e: Exception2) {
            val result: Boolean = syncFun2()
            if (result) {
                return suspendFun1()
            } else {
                return suspendFun2()
            }
        }
    }

    suspend fun suspendFun1(): Boolean {
        return true
    }

    private suspend fun suspendFun2(): Boolean {
        return true
    }

    private fun syncFun1(): Boolean {
        return false
    }

    private fun syncFun2(): Boolean {
        return false
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    builder {
        val a = A()
        a.execute()
    }

    return "OK"
}