// IGNORE_BACKEND: JS
// EXPECTED_REACHABLE_NODES: 1281

package foo

fun check(x: Any?) {
    x as Boolean
}

fun tests(x: Boolean, y: Boolean) {
    check(x)
    check(!x)
    check(x or y)
    check(x and y)
    check(x xor y)
    check(x.not())
    check(x || y)
    check(x && y)
}

fun box(): String {
    tests(false, false)
    tests(false, true)
    tests(true, false)
    tests(true, true)

    return "OK"
}