import kotlin.test.*
import kotlin.coroutines.*

// To be tested with -g.

// https://youtrack.jetbrains.com/issue/KT-49360

class Block(val block: () -> Int)

fun testWrapBlockCreate(flag: Boolean): Block {
    return (if (flag) Block { 11 } else null) ?: Block { 22 }
}

fun box(): String {
    assertEquals(11, testWrapBlockCreate(true).block())
    assertEquals(22, testWrapBlockCreate(false).block())

    return "OK"
}
