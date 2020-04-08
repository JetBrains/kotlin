import kotlin.reflect.KProperty

operator fun <T> List<T>.getValue(thisRef: T, property: KProperty<*>): Int = indexOf(thisRef)

class C {
    val property by <caret>
}

// EXIST: lazy
// EXIST: listOf
