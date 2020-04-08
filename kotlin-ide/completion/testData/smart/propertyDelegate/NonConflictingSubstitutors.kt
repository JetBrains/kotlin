import kotlin.reflect.KProperty

class X<T>(t: T) {
    operator fun getValue(thisRef: T, property: KProperty<*>): T = throw Exception()
}

class C<T> {
    val property: C<T> by <caret>
}

// EXIST: { itemText: "X", tailText: "(t: C<T>) (<root>)" }
