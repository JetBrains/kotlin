// EXPECTED_REACHABLE_NODES: 489
package foo

var i = 0

fun test(): Int? = i++

fun box(): String {
    if (i != 0) return "fail1: $i"
    test()?.plus(1)
    if (i != 1) return "fail2: $i"
    test()?.minus(2)
    if (i != 2) return "fail3: $i"
    return "OK"
}