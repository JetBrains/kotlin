// EXPECTED_REACHABLE_NODES: 1293
// CHECK_CONTAINS_NO_CALLS: testRangeTo
// CHECK_CONTAINS_NO_CALLS: testRangeToFunction
// CHECK_CONTAINS_NO_CALLS: testUntil
// CHECK_CONTAINS_NO_CALLS: testDownTo
// CHECK_CONTAINS_NO_CALLS: testStep
// CHECK_CONTAINS_NO_CALLS: testEmptyRange
// CHECK_CONTAINS_NO_CALLS: testRangeToParams except=from;to

fun testRangeTo(): String {
    var result = ""
    for (x in 1..3) {
        result += x
    }
    return result
}

fun testRangeToFunction(): String {
    var result = ""
    for (x in 1.rangeTo(3)) {
        result += x
    }
    return result
}

fun testUntil(): String {
    var result = ""
    for (x in 1 until 4) {
        result += x
    }
    return result
}

fun testDownTo(): String {
    var result = ""
    for (x in 3 downTo 1) {
        result += x
    }
    return result
}

fun testStep(): String {
    var result = ""
    for (x in 1..5 step 2) {
        result += x
    }
    result += ";"

    for (x in 1 until 6 step 2) {
        result += x
    }
    result += ";"

    for (x in 6.downTo(1).step(2)) {
        result += x
    }

    return result
}

fun testEmptyRange(): String {
    var result = ""
    for (x in 3..1) {
        result += x
    }
    return result
}

fun testRangeToParams(from: () -> Int, to: () -> Int): String {
    var result = ""
    for (x in (from()..to())) {
        result += x
    }
    return result
}

fun box(): String {
    var r = testRangeTo()
    if (r != "123") return "fail: rangeTo: $r"

    r = testRangeToFunction()
    if (r != "123") return "fail: rangeToFunction: $r"

    r = testUntil()
    if (r != "123") return "fail: until: $r"

    r = testDownTo()
    if (r != "321") return "fail: downTo: $r"

    r = testStep()
    if (r != "135;135;642") return "fail: $r"

    r = testEmptyRange()
    if (r != "") return "fail: emptyRange: $r"

    r = testRangeToParams({ 1 }, { 3 })
    if (r != "123") return "fail: rangeTo(1, 3): $r"

    var se = ""
    r = testRangeToParams({ se += "Q"; 1 }, { se += "W"; 3 })
    if (r != "123") return "fail: rangeTo(1, 3) with side effects: $r"
    if (se != "QW") return "fail: rangeTo(1, 3): side effects: $se"

    se = ""
    r = testRangeToParams({ se += "Q"; 3 }, { se += "W"; 1 })
    if (r != "") return "fail: rangeTo(1, 3) with side effects: $r"
    if (se != "QW") return "fail: rangeTo(1, 3): side effects: $se"

    return "OK"
}