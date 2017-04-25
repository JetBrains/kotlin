// EXPECTED_REACHABLE_NODES: 491
package foo

fun test(f: (Int) -> Boolean, p: Int) = f(p)

fun box(): String {
    if (!test({ it + 1 == 2 }, 1)) return "fail1"

    if (!test({ it > 1 }, 3)) return "fail2"

    return if (test({ ((it < 1) == false) }, 1)) "OK" else "fail3"

}