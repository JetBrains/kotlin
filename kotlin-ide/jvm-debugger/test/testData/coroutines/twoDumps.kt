package twoDumps

suspend fun main() {
    foo()
    sequence<Int> {
        foo()
        yield(666)
    }.toList()
}

suspend fun foo() {
    val f = 239
    bar()
}

suspend fun bar() {
    var r = 1337
    //Breakpoint!
    r += 42
}

suspend fun SequenceScope<Int>.foo() {
    val k = 228
    bar()
}

suspend fun SequenceScope<Int>.bar() {
    var r = 1337
    //Breakpoint!
    r += 42
}