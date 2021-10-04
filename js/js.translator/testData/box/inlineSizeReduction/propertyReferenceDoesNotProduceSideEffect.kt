// EXPECTED_REACHABLE_NODES: 1287

// FIXME: The IR backend generates a lot of redundant vars
// CHECK_VARS_COUNT: function=box count=1 TARGET_BACKENDS=JS

package foo

var log = ""

class A() {
    var x = 23
}

fun sideEffect(): Int {
    log += "sideEffect();"
    return 42
}

fun bar(a: Int, b: Int) {
    log += "bar($a, $b);"
}

inline fun test(a: Int, b: Int) {
    bar(b, a)
}

fun box(): String {
    val a = A()
    test(sideEffect(), a.x)
    assertEquals("sideEffect();bar(23, 42);", log)
    return "OK"
}