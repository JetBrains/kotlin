// DUMP_KT_IR

import kotlin.explain.*

fun box(): String {
    return test1() +
            test2()
}

fun test1() = expectThrowableMessage {
    @Explain val hello = "Hello"
    @Explain val world = "World".substring(1, 4)

    @Explain
    val expected =
        hello.length
    @Explain val actual = world.length
    assert(expected == actual)
}

fun test2() = expectThrowableMessage {
    test2_run()
}

@Explain
fun test2_run() {
    val expected = "Hello".length
    val actual = "World".substring(1, 4).length
    assert(expected == actual)
}
