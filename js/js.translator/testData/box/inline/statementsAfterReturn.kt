// EXPECTED_REACHABLE_NODES: 533
package foo

data class State(public var value: Int = 10)

inline fun withState(state: State, ext: State.() -> Unit) {
    state.ext()
    return
    state.value = 0
}

fun box(): String {
    val state = State()

    withState(state) {
        value = 111
    }

    assertEquals(111, state.value)

    return "OK"
}