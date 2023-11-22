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

infix fun ULong.ror(shift: Int): ULong = this shr shift

fun testUlong(): String {
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

value class UserValueClass(val raw: String)

fun testUseValueClass(): String {
    var result = "NOT OK"
    builder {
        val test = UserValueClass("test")
        val map = listOf(test).groupBy { it }.mapValues { "OK" }
        result = map[test] ?: "Cannot find value"
    }
    return result
}

fun box(): String {
    if (testUlong() != "OK") {
        return "Fail testUlong()"
    }
    if (testUseValueClass() != "OK") {
        return "Fail testUseValueClass()"
    }
    return "OK"
}
