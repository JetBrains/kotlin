import kotlin.test.*

fun inline_todo() {
    assertFailsWith<Throwable> {
        TODO("OK")
    }
}

fun inline_maxof() {
    assertEquals(17, maxOf(10, 17))
    assertEquals(17, maxOf(17, 13))
    assertEquals(17, maxOf(17, 17))
}

fun inline_assert() {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    assert(true)
}

fun box(): String {
    inline_todo()
    inline_assert()
    inline_maxof()
    return "OK"
}

