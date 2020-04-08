class X

class C {
    val property by D<caret>
}

// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
