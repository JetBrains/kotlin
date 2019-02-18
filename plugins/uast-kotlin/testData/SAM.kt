import java.lang.Thread
import java.lang.Runnable

import java.util.concurrent.Callable
import java.util.function.Supplier

val notSam = { /* Not SAM */ }
var foo: java.lang.Runnable = {/* Variable */}
fun bar(): java.lang.Runnable {
    foo = {/* Assignment */}
    val a = {/* Type Cast */} as java.lang.Runnable
    runRunnable {/* Argument */}
    return {/* Return */}
}

val baz = java.lang.Runnable { /* SAM */ }

fun runRunnable(r: java.lang.Runnable) = r()

fun test1() {
    val thread1 = Thread({ println("hello1") })
}

fun test2() {
    val thread2 = Thread(Runnable { println("hello2") })
}

fun test3() {
    ambiguousSamAcceptor(Supplier { "Supplier" })
    ambiguousSamAcceptor(Callable { "Callable" })
}

fun ambiguousSamAcceptor(s: Supplier<String>): String = TODO()
fun ambiguousSamAcceptor(s: Callable<String>): String = TODO()