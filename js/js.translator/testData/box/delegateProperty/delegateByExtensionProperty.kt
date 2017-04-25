// EXPECTED_REACHABLE_NODES: 541
package foo

import kotlin.reflect.KProperty

class State(var realValue: Int)

fun format(event: String, property: String, value: Int): String
    = "${event}: ${property} = ${value}; "

object LoggerDelegate {
    var log = ""

    operator fun getValue(state: State, desc: KProperty<*>): Int {
        log += format("get", desc.name, state.realValue)
        return state.realValue
    }

    operator fun setValue(state: State, desc: KProperty<*>, value: Int) {
        log += format("set", desc.name, value)
        state.realValue = value
    }
}

var State.value by LoggerDelegate

fun box(): String {
    val state = State(1)
    var expectedLog = ""

    assertEquals(1, state.value)
    expectedLog += format("get", "value", 1)

    state.value = 3
    expectedLog += format("set", "value", 3)

    assertEquals(3, state.value)
    expectedLog += format("get", "value", 3)

    assertEquals(expectedLog, LoggerDelegate.log)
    return "OK"
}
