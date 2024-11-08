// IGNORE_BACKEND_K1: ANY
// MODULE: lib
const val A = "Hello"
const val B = ", World"
const val WELCOME = A + B

// MODULE: main(lib)
fun box(): String {
    return test1() +
            test2() +
            test3() +
            test4() +
            test5() +
            test6() +
            test7()
}

fun test1() = expectThrowableMessage {
    assert(false)
}

const val x = false
fun test2() = expectThrowableMessage {
    assert(x)
}

fun test3() = expectThrowableMessage {
    assert(1 == 2)
}

const val y = 1
fun test4() = expectThrowableMessage {
    assert(y == 2)
}

fun test5() = expectThrowableMessage {
    assert(("Hello" + ", " + "World").length == 0)
}

const val GREETING = "Hello"
fun test6() = expectThrowableMessage {
    assert("$GREETING, World".length == 0)
}

fun test7() = expectThrowableMessage {
    assert(WELCOME.length == 0)
}
