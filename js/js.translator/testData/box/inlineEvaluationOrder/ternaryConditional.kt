// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_NOT_CALLED: bar_vux9f0$

inline fun bar(n: Int, x: Int) = if (n <= 5) x else 10

fun test(n: Int): Int = bar(n, fizz(n))

fun box(): String {
    var result = test(4)
    if (result != 4) return "fail1: $result"

    result = test(8)
    if (result != 10) return "fail2: $result"

    var log = pullLog()
    if (log != "fizz(4);fizz(8);") return "fail_log: $log"

    return "OK"
}