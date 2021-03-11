import kotlin.reflect.KProperty

class X<T> {
    operator fun getValue(thisRef: T, property: KProperty<*>): T = throw Exception()
}

class C<T> {
    val property: Int by <caret>
}

// ABSENT: X
