// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {

    if (1 in -2..0) return "fail1"
    if (1 in -10..-4) return "fail2"
    if (!(1 in 0..2)) return "fail3"

    if (!(1 in 1..2)) return "fail4"
    if (!(1 in -2..5)) return "fail5"

    return "OK"
}
