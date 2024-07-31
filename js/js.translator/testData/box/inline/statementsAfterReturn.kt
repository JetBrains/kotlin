// EXPECTED_REACHABLE_NODES: 1290
package foo

data class State(public var value: Int = 10)

inline fun withState(state: State, ext: State.() -> Unit) {
    state.ext()
    return
    state.value = 0
}

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$block count=0
fun box(): String {
    val state = State()

    withState(state) {
        value = 111
    }

    assertEquals(111, state.value)

    return "OK"
}