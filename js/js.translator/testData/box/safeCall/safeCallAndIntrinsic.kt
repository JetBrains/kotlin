// EXPECTED_REACHABLE_NODES: 490
package foo

var c1 = 0;

fun getInt(): Int? {
    c1++
    return c1
}

fun getNullInt(): Int? = null

fun box(): String {
    if (c1 != 0) {
        return "Start value of counter not 0, it: $c1"
    }
    val nullRes = getNullInt()?.toString()
    if (nullRes != null) {
        return "Broken safeCall. nullRes: $nullRes"
    }
    val res = getInt()?.toString()
    if (res != "1") {
        return "res != 1, it: $res, and c1: $c1"
    }
    if (c1 != 1) {
        return "Side effect. c1 != 1, it: $c1"
    }
    return "OK"
}