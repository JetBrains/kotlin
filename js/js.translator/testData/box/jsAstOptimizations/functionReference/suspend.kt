// CHECK_OPTIMIZED_JS
// WITH_STDLIB

import kotlin.coroutines.*

// ES5 suspend factories are KFunctionImpl constructors, not constructCallableReference,
// so invoke-only unwrap is ES6-only today.
// EXPECT_GENERATED_JS: function=u_susp$ref;k_susp$ref expect=suspend.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=u_susp$ref;k_susp$ref expect=suspend.es6.js TARGET_BACKENDS=JS_IR_ES6

suspend fun u_susp() = 1

suspend fun k_susp() = 2

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var unwrapped = 0
    builder {
        val f = ::u_susp
        unwrapped = f()
    }
    if (unwrapped != 1) return "fail unwrap"

    val g = ::k_susp
    if (g != ::k_susp) return "fail equals"
    if (g.name != "k_susp") return "fail name"

    return "OK"
}
