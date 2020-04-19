package coroutine1

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun main() {
    val cnt = Continuation<Int>(EmptyCoroutineContext) { }
    val result = ::test1.startCoroutine(1, cnt)
    println(result)
}

suspend fun test1(i: Int): Int {
    val test1 = "a"
    a(test1)
    return i
}

suspend fun a(aParam: String) {
    val a = "a"
    b(a)
    a + 1
}

suspend fun b(bParam: String) {
    val b = "b"
    //Breakpoint!
    b + 1
}