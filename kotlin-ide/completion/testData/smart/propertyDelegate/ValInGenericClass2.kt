import kotlin.reflect.KProperty

class X<T>(t: T) {
    operator fun getValue(thisRef: C<T>, property: KProperty<*>): T = throw Exception()
}

class C<T> {
    val property: T by <caret>
}

// EXIST: lazy
// EXIST: { itemText: "X", tailText: "(t: T) (<root>)" }
