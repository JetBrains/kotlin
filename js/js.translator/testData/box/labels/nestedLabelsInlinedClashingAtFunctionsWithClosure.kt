// EXPECTED_REACHABLE_NODES: 527
package foo

// CHECK_LABELS_COUNT: function=test name=loop count=1
// CHECK_LABELS_COUNT: function=test name=loop_0 count=1
// CHECK_LABELS_COUNT: function=test name=loop_1 count=1

class State() {
    public var value: Int = 0
}

internal fun test(state: State) {
    inline fun test3() {
        inline fun test2() {
            inline fun test1() {
                loop@ for (i in 1..10) {
                    state.value++
                    if (i == 2) break@loop
                }
            }

            loop@ for (i in 1..10) {
                test1()
                if (i == 2) break@loop
            }
        }

        loop@ for (i in 1..10) {
            test2()
            if (i == 2) break@loop
        }
    }

    test3()
}

fun box(): String {
    val state = State()
    test(state)
    assertEquals(8, state.value)

    return "OK"
}