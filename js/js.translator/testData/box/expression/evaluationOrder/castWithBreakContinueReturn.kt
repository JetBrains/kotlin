// EXPECTED_REACHABLE_NODES: 504
package foo

fun castWithReturn(): Int {
    return (return 23) as Int
}

fun safeCastWithReturn(): Int? {
    return (return 23) as Int?
}

fun isWithReturn(): Int {
    return if ((return 23) is Int) 2 else 3
}

fun castWithBreak(): Int {
    var result = 23
    while (result < 40) {
        if ((break as Int) == 0) result += 2 else result += 3
    }
    return result
}

fun safeCastWithBreak(): Int {
    var result = 23
    while (result < 40) {
        if ((break as Int) == 0) result += 2 else result += 3
    }
    return result
}

fun isWithBreak(): Int {
    var result = 23
    while (result < 40) {
        if (break is Int) result += 2 else result += 3
    }
    return result
}

fun castWithContinue(): Int {
    var result = 25
    while (result in 24..30) {
        --result
        if ((continue as Int) == 0) result += 2 else result += 3
    }
    return result
}

fun castNullableWithContinue(): Int {
    var result = 25
    while (result in 24..30) {
        --result
        if ((continue as Int?) == 0) result += 2 else result += 3
    }
    return result
}

fun safeCastWithContinue(): Int {
    var result = 25
    while (result in 24..30) {
        --result
        if ((continue as? Int) == 0) result += 2 else result += 3
    }
    return result
}

fun safeCastNullableWithContinue(): Int {
    var result = 25
    while (result in 24..30) {
        --result
        if ((continue as? Int?) == 0) result += 2 else result += 3
    }
    return result
}

fun isWithContinue(): Int {
    var result = 25
    while (result in 24..30) {
        --result
        if (continue is Int) result += 2 else result += 3
    }
    return result
}

fun isNullableWithContinue(): Int {
    var result = 23
    while (result in 24..30) {
        --result
        if (continue is Int?) result += 2 else result += 3
    }
    return result
}

fun whenWithReturn(): Int {
    return when ((return 23) as Int) {
        is Int -> 24
        else -> 25
    }
}

fun box(): String {
    assertEquals(23, castWithReturn())
    assertEquals(23, safeCastWithReturn())
    assertEquals(23, isWithReturn())

    assertEquals(23, castWithBreak())
    assertEquals(23, safeCastWithBreak())
    assertEquals(23, isWithBreak())

    assertEquals(23, castWithContinue())
    assertEquals(23, safeCastWithContinue())
    assertEquals(23, isWithContinue())

    assertEquals(23, castNullableWithContinue())
    assertEquals(23, safeCastNullableWithContinue())
    assertEquals(23, isNullableWithContinue())

    assertEquals(23, whenWithReturn())

    return "OK"
}