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

fun box(): String {
    var result = A() + 1
    if (result != 11) return "fail: member operator"

    result = B() + 2
    if (result != 22) return "fail: extension operator"

    return "OK"
}
