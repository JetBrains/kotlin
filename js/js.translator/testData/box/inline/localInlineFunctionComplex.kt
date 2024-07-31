// EXPECTED_REACHABLE_NODES: 1291
package foo

// CHECK_CONTAINS_NO_CALLS: addToState

internal data class State(var count: Int = 0)

internal inline fun repeatAction(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

// CHECK_BREAKS_COUNT: function=addToState count=0
// CHECK_LABELS_COUNT: function=addToState name=$l$block count=0
internal fun addToState(state: State, a: Int, b: Int): Int {
    inline fun inc(a: Int): Int {
        return a + 1
    }

    inline fun inc1(a: Int): Int {
        return inc(a)
    }

    repeatAction(a)  {
        inline fun inc2(a: Int): Int {
            return inc1(a)
        }

        repeatAction(b) {
            inline fun State.inc() {
                count = inc2(count)
            }

            state.inc()
        }
    }

    return state.count
}

fun box(): String {
    assertEquals(20, addToState(State(), 4, 5))

    return "OK"
}