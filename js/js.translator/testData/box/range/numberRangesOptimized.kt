// EXPECTED_REACHABLE_NODES: 1135
// CHECK_CONTAINS_NO_CALLS: inRange
// CHECK_CONTAINS_NO_CALLS: inRange2
// CHECK_CONTAINS_NO_CALLS: inRange3
// CHECK_CONTAINS_NO_CALLS: inRange4

// CHECK_CONTAINS_NO_CALLS: inLongRange except=lessThanOrEqual;lessThan;fromInt
// CHECK_CONTAINS_NO_CALLS: inLongRange2 except=lessThanOrEqual;lessThan;fromInt
// CHECK_CONTAINS_NO_CALLS: inLongRange3 except=lessThanOrEqual;lessThan;fromInt
// CHECK_CONTAINS_NO_CALLS: inLongRange4 except=lessThanOrEqual;lessThan;fromInt
// CHECK_VARS_COUNT: function=inLongRange count=0
// CHECK_VARS_COUNT: function=inLongRange2 count=0
// CHECK_VARS_COUNT: function=inLongRange3 count=0
// CHECK_VARS_COUNT: function=inLongRange4 count=0

fun inRange(x: Int) = x in 1..10

fun inRange2(x: Int) = x in 1.rangeTo(10)

fun inRange3(x: Int) = x in 1 until 10

fun inRange4(x: Int) = 1.rangeTo(10).contains(x)

fun inLongRange(x: Long) = x in 1L..10L

fun inLongRange2(x: Long) = x in 1L.rangeTo(10L)

fun inLongRange3(x: Long) = x in 1L until 10L

fun inLongRange4(x: Long) = 1L.rangeTo(10L).contains(x)

fun check(x: Int, inRangeTo: Boolean, inUntil: Boolean): String? {
    val p = if (inRangeTo) "!" else ""
    if (inRange(x) != inRangeTo) return "fail: $x ${p}in 1..10"
    if (inRange2(x) != inRangeTo) return "fail: $x ${p}in 1.rangeTo(10)"
    if (inRange4(x) != inRangeTo) return "fail: ${p}1.rangeTo(10).contains($x)"

    if (inRange3(x) != inUntil) return "fail: $x ${if (inUntil) "!" else ""}in 1 until 10"

    return null
}

fun check(x: Long, inRangeTo: Boolean, inUntil: Boolean): String? {
    val p = if (inRangeTo) "!" else ""
    if (inLongRange(x) != inRangeTo) return "fail: ${x}L ${p}in 1L..10L"
    if (inLongRange2(x) != inRangeTo) return "fail: ${x}L ${p}in 1L.rangeTo(10L)"
    if (inLongRange4(x) != inRangeTo) return "fail: ${p}1L.rangeTo(10L).contains(${x}L)"

    if (inLongRange3(x) != inUntil) return "fail: ${x}L ${if (inUntil) "!" else ""}in 1L until 10L"

    return null
}

fun box(): String {
    // Int
    check(5, true, true)?.let { return it }
    check(9, true, true)?.let { return it }
    check(10, true, false)?.let { return it }
    check(11, false, false)?.let { return it }
    check(1, true, true)?.let { return it }
    check(0, false, false)?.let { return it }

    // Long
    check(5L, true, true)?.let { return it }
    check(9L, true, true)?.let { return it }
    check(10L, true, false)?.let { return it }
    check(11L, false, false)?.let { return it }
    check(1L, true, true)?.let { return it }
    check(0L, false, false)?.let { return it }

    return "OK"
}