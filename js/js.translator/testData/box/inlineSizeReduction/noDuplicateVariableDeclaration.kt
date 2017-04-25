// EXPECTED_REACHABLE_NODES: 488
package foo

// CHECK_VARS_COUNT: function=test count=3

inline fun if1(f: (Int) -> Int, a: Int, b: Int, c: Int): Int {
    val result = f(a)

    if (result == b) {
        return f(a)
    }

    return f(c)
}

fun test(x: Int): Int {
    val test1 = if1({ it }, x, 2, 3)
    return test1
}

fun box(): String {
    var result = test(2)
    if (result != 2) return "fail1: $result"

    result = test(100)
    if (result != 3) return "fail2: $result"

    return "OK"
}