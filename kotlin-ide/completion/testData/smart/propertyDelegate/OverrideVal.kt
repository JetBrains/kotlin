import kotlin.reflect.KProperty

abstract class Base {
    abstract val property: Number
}

class Derived : Base() {
    override val property by <caret>
}

class X1 {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Number = 1
}

class X2 {
    operator fun getValue(thisRef: Any, property: KProperty<*>): String = ""
}

class X3 {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Int = ""
}

// EXIST: lazy
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, Number>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: Number, crossinline onChange: (KProperty<*>, Number, Number) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, Number>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: Number, crossinline onChange: (KProperty<*>, Number, Number) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, Number>", attributes:"" }

// EXIST: X1
// ABSENT: X2
// EXIST: X3
