fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
)

fun test1() {
    assert(listOf("Hello", "World").contains("Name"))
}

fun test2() {
    val str = "a"
    assert("Hello".contains(str))
}

fun test3() {
    val str = "a"
    assert("Hello".contains(str, ignoreCase = true))
}

fun test4() {
    val char = 'a'
    assert("Hello".contains(char))
}

fun test5() {
    val char = 'a'
    assert("Hello".contains(char, ignoreCase = true))
}
