// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1378
package foo

fun box(): String {
    val x = "895065487315017728"
    if (" ${x.toLong()}" != " $x") return "FAIL 1"
    if (" ${x.toLong() as Comparable<Long>}" != " $x") return "FAIL 2"
    if (" ${x.toLong() as Number}" != " $x") return "FAIL 3"
    if (" ${x.toLong() as Any}" != " $x") return "FAIL 4"
    if (" ${x.toLong() as Long?}" != " $x") return "FAIL 5"
    if (" ${null as Long?}" != " null") return "FAIL 6"
    if (" ${x.toLong() as Comparable<Long>?}" != " $x") return "FAIL 7"
    if (" ${null as Comparable<Long>?}" != " null") return "FAIL 8"
    if (" ${x.toLong() as Number?}" != " $x") return "FAIL 9"
    if (" ${null as Number?}" != " null") return "FAIL 10"
    if (" ${x.toLong() as Any?}" != " $x") return "FAIL 11"
    if (" ${null as Any?}" != " null") return "FAIL 12"

    return "OK"
}