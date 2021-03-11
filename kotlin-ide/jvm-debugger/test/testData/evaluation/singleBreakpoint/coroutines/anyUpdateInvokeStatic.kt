package anyUpdateInvokeStatic

import kotlin.sequences.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit>{
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

object A {
    @JvmStatic var s: Any? = "aabb"
}

fun main(args: Array<String>) {
    builder {
        A.s = strChanger(A.s)
        //Breakpoint!
        println(A.s) // (1)
    }
}

suspend fun strChanger(str: Any?): Any? = (str as String).filter { it !in "a" }

// EXPRESSION: A.s
// RESULT: "bb": Ljava/lang/String;