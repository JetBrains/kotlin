package foo

// CHECK_CONTAINS_NO_CALLS: add

data class State(var count: Int = 0)

inline fun repeat(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

fun add(state: State, a: Int, b: Int): Int {
    [inline] fun inc(a: Int): Int {
        return a + 1
    }

    [inline] fun inc1(a: Int): Int {
        return inc(a)
    }

    repeat(a)  {
        [inline] fun inc2(a: Int): Int {
            return inc1(a)
        }

        repeat(b) {
            [inline] fun State.inc() {
                count = inc2(count)
            }

            state.inc()
        }
    }

    return state.count
}

fun box(): String {
    assertEquals(20, add(State(), 4, 5))

    return "OK"
}