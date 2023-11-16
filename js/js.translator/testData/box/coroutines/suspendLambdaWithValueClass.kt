// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun box(): String {
    val a: suspend () -> Unit = {
        mapOf(
            0x0000000000000000UL to 0x0000000000000000UL
        ).forEach { (base: ULong) ->
            0x0000000000000000UL.ror(2)
            base.ror(1)
        }
    }

    val b: () -> Unit = {
        mapOf(
            0x0000000000000000UL to 0x0000000000000000UL
        ).forEach { (base: ULong) ->
            0x0000000000000000UL.ror(2)
            base.ror(1)
        }
    }

    builder {
        a()
        b()
    }

    return "OK"
}

infix fun ULong.ror(shift: Int): ULong = this shr shift