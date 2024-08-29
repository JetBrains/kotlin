// DUMP_KT_IR

import kotlin.explain.*

@ExplainCall
fun assertEquals(expected: Any?, actual: Any?, @ExplainIgnore message: String? = null) {
    if (actual != expected) {
        val diagram = ExplainCall.explanation ?: error("no power-assert")
        throw AssertionError("\n" + diagram.toDefaultMessage())
    }
}

fun box(): String {
    return test1() +
            test2() +
            test3()
}

fun test1() = expectThrowableMessage {
    assertEquals("Hello".length, "World".substring(1, 4).length)
}

fun test2() = expectThrowableMessage {
    assertEquals("Hello".length, "World".substring(1, 4).length, message = "Values are not equal!")
}

fun test3() = expectThrowableMessage {
    val message = "Values are not equal!"
    assertEquals("Hello".length, "World".substring(1, 4).length, message)
}
