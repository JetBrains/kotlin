
// SNIPPET

import kotlin.reflect.KProperty

class A

class CustomDelegate {
    private var value: String = "OK"

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        value = newValue
    }
}

// SNIPPET

val A.x by CustomDelegate()

// SNIPPET

val y = A().x

// EXPECTED: y == "OK"

