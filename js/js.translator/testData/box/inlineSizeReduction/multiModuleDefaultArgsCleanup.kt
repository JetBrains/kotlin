// EXPECTED_REACHABLE_NODES: 1281
// MODULE: lib
// FILE: lib.kt
inline fun foo(x: String = "x", y: String = "y") = x + y

// MODULE: main(lib)
// FILE: main.kt

// FIXME: The IR backend generates a lot of redundant vars
// CHECK_VARS_COUNT: function=test count=0 TARGET_BACKENDS=JS

fun test() = foo() + ";" + foo(x = "X") + ";" + foo(y = "Y") + ";" + foo(x = "X", y = "Y")

fun box(): String {
    val r = test()
    if (test() != "xy;Xy;xY;XY") return "fail: $r"

    return "OK"
}
