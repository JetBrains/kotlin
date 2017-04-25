// EXPECTED_REACHABLE_NODES: 489
// MODULE: lib
// FILE: lib.kt

class A {
    inline operator fun plus(a: Int) = a + 10
}

class B

inline operator fun B.plus(b: Int) = b + 20

// MODULE: main(lib)
// FILE: main.kt

// CHECK_NOT_CALLED_IN_SCOPE: function=plus_za3lpa$ scope=box
// CHECK_NOT_CALLED_IN_SCOPE: function=plus scope=box

fun box(): String {
    var result = A() + 1
    if (result != 11) return "fail: member operator"

    result = B() + 2
    if (result != 22) return "fail: extension operator"

    return "OK"
}