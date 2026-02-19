// KT-54134
// WITH_STDLIB

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

external interface Test {
    val test: String
}

suspend fun <T> smth(xx: T): T = xx

suspend fun foo(): Test {
    val x1 = smth(js("{}"))
    val node = js("Object.assign({ test: 'O' }, x1)")
    return smth(node)
}
suspend fun bar(): Test {
    val x1 = smth(js("{}"))
    val node = js("""
        x1.test = 'K'
        Object.assign({}, x1)
    """)
    return smth(node)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        result += foo().test
        result += bar().test
    }

    return result
}