// FREE_COMPILER_ARGS: -Xdisable-phases=CoroutinesLivenessAnalysis
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class Data(val x: Int)

fun box(): String {
    var result = ""

    builder {
        var data = Data(41)
        suspendHere()
        data = Data(data.x + 1)
        suspendHere()
        result = if (data.x == 42) "OK" else "fail"
    }

    return result
}
