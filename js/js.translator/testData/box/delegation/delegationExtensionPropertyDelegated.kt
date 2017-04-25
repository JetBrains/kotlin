// EXPECTED_REACHABLE_NODES: 547
package foo

import kotlin.reflect.KProperty

class State(var value: Int)

interface Base {
    var State.multiplied: Int
}

class Delegate(val multiplier: Int) {
    operator fun getValue(state: State, desc: KProperty<*>): Int  = multiplier * state.value

    operator fun setValue(state: State, desc: KProperty<*>, value: Int) {
        state.value = value / multiplier
    }

}

open class BaseImpl() : Base {
    override var State.multiplied: Int by Delegate(2)
}

class Derived() : Base by BaseImpl() {
    fun getValueMultiplied(state: State): Int = state.multiplied

    fun setValueMultiplied(state: State, value: Int) {
        state.multiplied = value
    }
}

fun box(): String {
    val d = Derived()

    val state = State(2)
    assertEquals(4, d.getValueMultiplied(state))

    d.setValueMultiplied(state, 10)
    assertEquals(10, d.getValueMultiplied(state))

    return "OK"
}
