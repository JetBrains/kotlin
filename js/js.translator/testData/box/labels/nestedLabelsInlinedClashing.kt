// EXPECTED_REACHABLE_NODES: 527
package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_LABELS_COUNT: function=test name=loop count=1
// CHECK_LABELS_COUNT: function=test name=loop_0 count=1
// CHECK_LABELS_COUNT: function=test name=loop_1 count=1

class State() {
    public var value: Int = 0
}

internal inline fun test1(state: State) {
    loop@ for (i in 1..10) {
        state.value++
        if (i == 2) break@loop
    }
}

internal inline fun test2(state: State) {
    loop@ for (i in 1..10) {
        test1(state)
        if (i == 2) break@loop
    }
}

internal inline fun test3(state: State) {
    loop@ for (i in 1..10) {
        test2(state)
        if (i == 2) break@loop
    }
}

internal fun test(state: State) {
    test3(state)
}

fun box(): String {
    val state = State()
    test(state)
    assertEquals(8, state.value)

    return "OK"
}