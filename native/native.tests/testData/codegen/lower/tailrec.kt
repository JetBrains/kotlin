// FREE_COMPILER_ARGS: -XXLanguage:-ProhibitTailrecOnVirtualMember

import kotlin.test.*

fun box(): String {
    assertEquals(12, add(5, 7))
    assertEquals(100000000, add(100000000, 0))

    assertEquals(8, fib(6))

    assertEquals(1, one(5))

    countdown(3)
    assertEquals("""
        3 ...
        2 ...
        1 ...
        ready!
    """.trimIndent(), sb.toString())

    assertEquals(2, listOf(1, 2, 3).indexOf(3))
    assertEquals(-1, listOf(1, 2, 3).indexOf(4))
    return "OK"
}

tailrec fun add(x: Int, y: Int): Int = if (x > 0) add(x - 1, y + 1) else y

fun fib(n: Int): Int {
    tailrec fun fibAcc(n: Int, acc: Int): Int = if (n < 2) {
        n + acc
    } else {
        fibAcc(n - 1, fibAcc(n - 2, acc))
    }

    return fibAcc(n, 0)
}

tailrec fun one(delay: Int, result: Int = delay + 1): Int = if (delay > 0) one(delay - 1) else result

val sb = StringBuilder()
tailrec fun countdown(iterations: Int): Unit {
    if (iterations > 0) {
        sb.appendLine("$iterations ...")
        countdown(iterations - 1)
    } else {
        sb.append("ready!")
    }
}

tailrec fun <T> List<T>.indexOf(x: T, startIndex: Int = 0): Int {
    if (startIndex >= this.size) {
        return -1
    }

    if (this[startIndex] != x) {
        return this.indexOf(x, startIndex + 1)
    }

    return startIndex

}
