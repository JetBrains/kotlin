// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1625
// MODULE: lib
// FILE: lib.kt


inline fun tenUInt() = 10U

inline fun tenULong() = 10UL

// MODULE: main(lib)
// FILE: main.kt

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box(): String {

    if (tenUInt() != 10U) return "fail 1"

    if (tenULong() != 10UL) return "fail 2"

    return "OK"
}
