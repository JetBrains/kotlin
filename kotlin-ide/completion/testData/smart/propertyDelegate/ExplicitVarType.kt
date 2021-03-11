import kotlin.reflect.KProperty

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

class X2 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: CharSequence) {}
}

class X3 {
    operator fun getValue(thisRef: C, property: KProperty<*>): CharSequence = ""
    operator fun setValue(thisRef: C, property: KProperty<*>, value: String) {}
}

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()

class C {
    var property: CharSequence by <caret>
}

// ABSENT: lazy
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: CharSequence, crossinline onChange: (KProperty<*>, CharSequence, CharSequence) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: CharSequence, crossinline onChange: (KProperty<*>, CharSequence, CharSequence) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, CharSequence>", attributes:"" }
// ABSENT: createX1
// EXIST: createX2
// ABSENT: createX3
