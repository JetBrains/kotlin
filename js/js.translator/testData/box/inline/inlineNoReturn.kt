// EXPECTED_REACHABLE_NODES: 528
package foo

// CHECK_CONTAINS_NO_CALLS: factAbsNoInline1 except=imul

internal class State(value: Int) {
    public var value: Int = value
}

internal inline fun multiply(state: State, factor: Int) {
    state.value *= factor
}

internal inline fun abs(state: State) {
    val value = state.value
    if (value < 0) {
        multiply(state, -1)
    }
}

internal inline fun factAbs(state: State) {
    abs(state)

    if (state.value == 0) {
        state.value = 1
        return
    }

    var n = state.value
    while (n > 1) {
        n--
        multiply(state, n)
    }
}

internal fun factAbsNoInline1(state: State): Int {
    factAbs(state)
    return state.value
}

internal fun factAbsNoInline2(n: Int): Int {
    return factAbsNoInline1(State(n))
}

fun box(): String {
    assertEquals(1, factAbsNoInline2(0))
    assertEquals(2, factAbsNoInline2(2))
    assertEquals(6, factAbsNoInline2(-3))
    assertEquals(120, factAbsNoInline2(5))
    assertEquals(720, factAbsNoInline2(-6))

    return "OK"
}