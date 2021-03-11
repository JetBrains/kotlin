// FLOW: IN

import kotlin.reflect.KProperty

object D {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 1
}

val foo: Int by D

fun test() {
    val <caret>x = foo
}