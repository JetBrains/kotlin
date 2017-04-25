// EXPECTED_REACHABLE_NODES: 535
package foo

// CHECK_CONTAINS_NO_CALLS: capturedInLambda
// CHECK_CONTAINS_NO_CALLS: declaredInLambda

internal data class State(var count: Int = 0)

internal inline fun repeatAction(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

internal fun capturedInLambda(state: State, a: Int, b: Int): Int {
    inline fun State.inc() {
        count++
    }

    repeatAction(a + b)  {
        state.inc()
    }

    return state.count
}


internal fun declaredInLambda(state: State, a: Int, b: Int): Int {
    repeatAction(a)  {
        inline fun State.inc() {
            count++
        }

        repeatAction(b) {
            state.inc()
        }
    }

    return state.count
}


fun box(): String {
    assertEquals(3, capturedInLambda(State(), 1, 2), "capturedInLambda")
    assertEquals(9, capturedInLambda(State(), 4, 5), "capturedInLambda")

    assertEquals(2, declaredInLambda(State(), 1, 2), "declaredInLambda")
    assertEquals(20, declaredInLambda(State(), 4, 5), "declaredInLambda")

    return "OK"
}