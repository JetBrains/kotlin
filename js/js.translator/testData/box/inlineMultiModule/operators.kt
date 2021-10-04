// EXPECTED_REACHABLE_NODES: 1284
// MODULE: lib
// FILE: lib.kt

class A {
    inline operator fun plus(a: Int) = a + 10
}

class B

inline operator fun B.plus(b: Int) = b + 20

// MODULE: main(lib)
// FILE: main.kt

// CHECK_NOT_CALLED_IN_SCOPE: function=plus_za3lpa$ scope=box TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=plus scope=box TARGET_BACKENDS=JS

// CHECK_FUNCTION_EXISTS: plus_ha5a7z_k$ IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=plus_ha5a7z_k$ scope=box IGNORED_BACKENDS=JS
// CHECK_FUNCTION_EXISTS: plus_0 IGNORED_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=plus_0 scope=box IGNORED_BACKENDS=JS

fun box(): String {
    var result = A() + 1
    if (result != 11) return "fail: member operator"

    result = B() + 2
    if (result != 22) return "fail: extension operator"

    return "OK"
}