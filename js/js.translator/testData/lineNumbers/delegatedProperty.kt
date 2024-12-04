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

// LINES(ClassicFrontend JS_IR): 3 3 5 5 4 5 5 5 4 1 4 5 4 5 5 5 4 1 4 8 8 9 10 10 13 14 14 4 4 20 * 4 20 * 4 4 20 * 4 20
// LINES(FIR JS_IR):             3 3 5 5 4 5 5 5 5 5 5 8 8 9 10 10 13 14 14 5 5 20 * 5 20 * 5 5 20 * 5 20
