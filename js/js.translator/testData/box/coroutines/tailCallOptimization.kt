// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// EXPECT_GENERATED_JS: function=delay expect=tailCallOptimization.out.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=delay expect=tailCallOptimization.out.es6.js TARGET_BACKENDS=JS_IR_ES6
public suspend fun delay(timeMillis: Long) {
    if (timeMillis <= 0) return // don't delay
    return suspendCancellableCoroutine {}
}

public suspend inline fun suspendCancellableCoroutine(
    crossinline block: () -> Unit
) = suspendCoroutineUninterceptedOrReturn<Unit> { uCont ->
    block()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        delay(1000)
    }

    return "OK"
}
