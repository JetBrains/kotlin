// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1229
package foo

fun box(): String {
    val x = "895065487315017728"
    if (" ${x.toLong()}" != " $x") return "FAIL 1"
    if (" ${x.toLong() as Comparable<Long>}" != " $x") return "FAIL 2"
    if (" ${x.toLong() as Number}" != " $x") return "FAIL 3"
    if (" ${x.toLong() as Any}" != " $x") return "FAIL 4"
    if (" ${x.toLong() as Long?}" != " $x") return "FAIL 5"
    if (" ${x.toLong() as Comparable<Long>?}" != " $x") return "FAIL 6"
    if (" ${x.toLong() as Number?}" != " $x") return "FAIL 7"
    if (" ${x.toLong() as Any?}" != " $x") return "FAIL 8"

    return "OK"
}