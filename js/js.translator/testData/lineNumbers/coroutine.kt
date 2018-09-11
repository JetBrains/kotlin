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

// LINES: 40 40 4 4 4 5 5 45 45 5 86 45 5 5 6 4 4 4 15 9 9 9 9 15 9 9 9 * 15 10 10 11 11 11 2 11 11 * 11 12 12 13 13 13 2 13 13 13 13 14 14