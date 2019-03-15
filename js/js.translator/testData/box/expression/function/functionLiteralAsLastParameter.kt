// EXPECTED_REACHABLE_NODES: 1283
package foo

fun f(a: (Int) -> Int) = a(1)

fun box(): String {

    if (f() {
        it + 2
    } != 3) return "fail1"

    if (f() { a: Int -> a * 300 } != 300) return "fail2"

    return "OK"
}