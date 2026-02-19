
// SNIPPET

import kotlin.reflect.KProperty

class A
class B

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

var B.x by CustomDelegate()

// SNIPPET

B().x = B().x.lowercase()

val y = A().x + B().x

// EXPECTED: y == "OKok"
