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

// LINES: 3 4 5 4 4 4 2 * 8 11 10 10 15 14 14