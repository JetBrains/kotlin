import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

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

// LINES: 36 36 4 45 45 15 9 9 9 9 15 9 9 9 * 15 10 10 11 11 11 11 11 * 11 12 12 13 13 13 13 13 13 13 14 14