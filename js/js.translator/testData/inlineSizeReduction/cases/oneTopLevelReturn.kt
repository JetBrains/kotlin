package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_VARS_COUNT: function=test count=0

var log = ""

inline fun run1(fn: ()->Int): Int {
    log += "1;"
    return 1 + fn()
}

inline fun run2(fn: ()->Int): Int {
    log += "2;"
    return 2 + run1(fn)
}

inline fun run3(fn: ()->Int): Int {
    log += "3;"
    return 3 + run2(fn)
}

fun test(x: Int): Int = run3 { x }

fun box(): String {
    assertEquals(7, test(1))
    assertEquals("3;2;1;", log)

    return "OK"
}