fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2() },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5() },
    "test6" to { test6() },
    "test7" to { test7() },
)

fun test1() {
    assert("Name" in listOf("Hello", "World"))
}

fun test2() {
    // Test that we don't just search for `in` in the expression.
    assert(" in " in listOf("Hello", "World"))
}

fun test3() {
    // Test multiline case
    assert(
        " in "

                        in

                   listOf("Hello", "World")
    )
}

fun test4() {
    // Test that we don't assume whitespaces around the infix operator
    assert("Name"/*in*/in/*in*/listOf("Hello", "World"))
}

fun test5() {
    // Test nested `in`
    assert(("Name" in listOf("Hello", "World")) in listOf(true))
}

fun test6() {
    val str = "a"
    assert(str in "Hello")
}

fun test7() {
    val char = 'a'
    assert(char in "Hello")
}
