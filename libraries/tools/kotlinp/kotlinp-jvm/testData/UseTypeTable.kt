// USE_TYPE_TABLE

import kotlin.reflect.KProperty

class Delegate<T>(val value: T? = null) {
    operator fun getValue(instance: Any?, property: KProperty<*>): T = value!!
}

fun <T> foo(x: T): T {
    val y by Delegate<String>("")
    return x
}
