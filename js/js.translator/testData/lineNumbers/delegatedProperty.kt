import kotlin.reflect.*

open class A {
    var x: String by
            B()
}

class B {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "OK"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println(value)
    }
}

// LINES(JS):      3 4 5 * 4 4 4 *                               8 9 11 10 10 13 15 14 14
// LINES(JS_IR): 3 3   5 5 4       5 5 5 4 1 4 5 4 5 5 5 4 1 4 8 8 9    10 10 13    14 14 4 4 20 * 4 20 * 4 4 20 * 4 20
