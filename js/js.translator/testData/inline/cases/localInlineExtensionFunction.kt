package foo

// CHECK_CONTAINS_NO_CALLS: capturedInLambda
// CHECK_CONTAINS_NO_CALLS: declaredInLambda

data class State(var count: Int = 0)

inline fun repeat(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

fun capturedInLambda(state: State, a: Int, b: Int): Int {
    [inline] fun State.inc() {
        count++
    }

    repeat(a + b)  {
        state.inc()
    }

    return state.count
}


fun declaredInLambda(state: State, a: Int, b: Int): Int {
    repeat(a)  {
        [inline] fun State.inc() {
            count++
        }

        repeat(b) {
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