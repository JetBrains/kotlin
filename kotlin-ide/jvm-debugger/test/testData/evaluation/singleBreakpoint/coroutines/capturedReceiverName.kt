package capturedReceiverName

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

fun main(args: Array<String>) {
    builder {
        var s = "OK"
        s = strChanger(s) { character ->
            //Breakpoint!
            character != 'a' // (2)
        }
        println(s)
    }
}

suspend fun strChanger(str: String, pred: suspend (Char) -> Boolean): String {
    var result = ""
    str.forEach {
        if (pred(it)) {
            result += it
        }
    }
    return result
}

// EXPRESSION: character
// RESULT: 79: C
// 79.toChar() == 'O'