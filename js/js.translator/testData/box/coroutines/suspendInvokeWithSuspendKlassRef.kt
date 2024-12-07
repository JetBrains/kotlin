// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

private var stopped = false

fun build(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override fun resumeWith(x: Result<Unit>) {
            stopped = true
        }

        override val context = EmptyCoroutineContext
    })
}

inline fun <reified T : Any> klassToString() = T::class.toString()

typealias SusFun0 = suspend () -> Unit
typealias SusFun1 = suspend (String) -> Unit
typealias SusFun2 = suspend (String, String) -> Unit

val SusFun0Prop = klassToString<SusFun0>()
val SusFun1Prop = klassToString<SusFun1>()
val SusFun2Prop = klassToString<SusFun2>()

class Foo {
    suspend fun susFun0(
        parse: suspend () -> String
    ): String {
        return "0" + parse()
    }

    suspend fun susFun1(
        parse: suspend (String) -> String
    ): String {
        return "1" + parse("a")
    }

    suspend fun susFun2(
        parse: suspend (String, String) -> String
    ): String {
        return "2" + parse("a", "b")
    }
}



fun box(): String {
    SusFun0Prop
    SusFun1Prop
    SusFun2Prop

    var log = ""
    val foo = Foo()

    build {
        val foo0 = foo.susFun0 {
            "0"
        }

        log += foo0

        val foo1 = foo.susFun1 { one ->
            one + "1"
        }

        log += foo1

        val foo2 = foo.susFun2 { one, two ->
            one + two + "2"
        }

        log += foo2
    }

    while (!stopped) {
    }

    if (log != "001a12ab2") return "fail: $log"

    return "OK"
}