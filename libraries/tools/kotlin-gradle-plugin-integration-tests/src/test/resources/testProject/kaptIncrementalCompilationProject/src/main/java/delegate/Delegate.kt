package delegate

import kotlin.reflect.KProperty

class Delegate() {
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "I'm your val"
    }

    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in $thisRef.")
    }
}
