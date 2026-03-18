// DUMP_KT_IR

import kotlinx.powerassert.*

@PowerAssert
fun assertEquals(expected: Any?, actual: Any?, @PowerAssert.Ignore message: String? = null) {
    if (actual != expected) {
        val diagram = PowerAssert.explanation ?: error("no power-assert")
        throw AssertionError("${message ?: ""}\n" + diagram.toDefaultMessage())
    }
}

fun box(): String  = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
)

fun test1() {
    assertEquals("Hello".length, "World".substring(1, 4).length)
}

fun test2() {
    assertEquals("Hello".length, "World".substring(1, 4).length, message = "Values are not equal!")
}

fun test3() {
    val message = "Values are not equal!"
    assertEquals("Hello".length, "World".substring(1, 4).length, message)
}
