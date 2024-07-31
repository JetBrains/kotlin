// EXPECTED_REACHABLE_NODES: 1281
// MODULE: lib
// FILE: lib.kt
inline fun foo(x: String = "x", y: String = "y") = x + y

// MODULE: main(lib)
// FILE: main.kt

fun test() = foo() + ";" + foo(x = "X") + ";" + foo(y = "Y") + ";" + foo(x = "X", y = "Y")

fun box(): String {
    val r = test()
    if (test() != "xy;Xy;xY;XY") return "fail: $r"

    return "OK"
}
