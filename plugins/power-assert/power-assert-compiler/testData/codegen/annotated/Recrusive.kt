// DUMP_KT_IR

import kotlinx.powerassert.*

@PowerAssert
fun recursive(n: Int): String? {
    if (n <= 0) return PowerAssert.explanation?.toDefaultMessage()
    return recursive(n - 1)
}

fun box(): String = runAllOutput(
    "test0" to ::test0,
    "test1" to ::test1,
    "test2" to ::test2,
    "test100" to ::test100,
)

fun test0(): String {
    return recursive(0) ?: "no description"
}

fun test1(): String {
    return recursive(1) ?: "no description"
}

fun test2(): String {
    return recursive(2) ?: "no description"
}

fun test100(): String {
    return recursive(100) ?: "no description"
}
