// SNIPPET

import kotlin.reflect.KProperty

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

val str by CustomDelegate()

str

// EXPECTED: <res> == "OK"
