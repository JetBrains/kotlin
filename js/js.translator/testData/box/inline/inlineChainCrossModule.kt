// EXPECTED_REACHABLE_NODES: 1368

// MODULE: lib
// FILE: lib.kt


inline fun baz() = pp()

fun pp(): String = "OK"

// MODULE: mid(lib)
// FILE: mid.kt

fun bar() {
    foo()
}

inline fun foo()= baz()

// MODULE: main(mid)
// FILE: main.kt

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box() = foo()
