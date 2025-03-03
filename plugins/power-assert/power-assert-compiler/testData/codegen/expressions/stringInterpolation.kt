// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiDollarInterpolation
fun box(): String = runAll(
    "test1" to { test1("Joe") },
    "test2" to { test2("Joe") },
    "test3" to { test3(listOf("a","b")) },
    "test4" to { test4("Joe") },
    "test5" to { test5() },
    "test6" to { test6("Joe") },
)

fun test1(a: String) {
    assert("Hello, $a" == "string")
}

fun test2(a: String) {
    assert("${a.length}" == "5")
}

fun test3(a: List<String>) {
    assert("List: $a" == "")
}

fun test4(a: String) {
    assert("Escaping: \$ $a" == "")
}

fun test5() {
    assert("""Multiline escaping: ${'$'}_9.99 """ == "price")
}

fun test6(a: String) {
    assert($$"""Multidollar interpolation $a : $$a""" == "")
}