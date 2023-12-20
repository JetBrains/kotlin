import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(value: Int): Int = suspendCoroutineUninterceptedOrReturn { c ->
    c.resume(value)
    COROUTINE_SUSPENDED
}

suspend fun bar(): Unit {
    println("!")
    val a = foo(2)
    println("!")
    val b = foo(3)
    println(a + b)
}

// LINES(JS_IR): 4 4 * 93 3 45 45 7 7 6 9 9 * 9 * 9 * 10 10 * 11 * 11 12 12 * 13 * 13 14 14 15 15
