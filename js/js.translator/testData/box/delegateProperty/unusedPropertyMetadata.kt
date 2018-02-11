// EXPECTED_REACHABLE_NODES: 1131
// PROPERTY_NOT_USED: PropertyMetadata
import kotlin.reflect.KProperty

class MyDelegate(val value: String) {
    inline operator fun getValue(receiver: Any?, property: KProperty<*>): String = value
}

fun box(): String {
    val x by MyDelegate("OK")
    return x
}