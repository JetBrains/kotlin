import kotlin.reflect.KProperty

class X1 {
    operator fun getValue(thisRef: C, property: KProperty<*>): String = ""

    companion object {
        fun create() = X1()
    }
}
class X2 {
    operator fun getValue(thisRef: String, property: KProperty<*>): String = ""
}
class X3 {
    operator fun getValue(thisRef: Any, property: KProperty<*>): String = ""
}

class Y1
class Y2
abstract class Y3

operator fun Y1.getValue(thisRef: C, property: KProperty<*>): String = ""
operator fun Y2.getValue(thisRef: String, property: KProperty<*>): String = ""
operator fun Y3.getValue(thisRef: Any, property: KProperty<*>): String = ""

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()
fun createY1() = Y1()
fun createY2() = Y2()
fun createY3(): Y3 = object : Y3() {}

class C {
    val property by <caret>
}

// EXIST: lazy
// EXIST: { itemText: "Delegates.notNull", tailText:"() (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.observable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Unit) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }
// EXIST: { itemText: "Delegates.vetoable", tailText:"(initialValue: T, crossinline onChange: (KProperty<*>, T, T) -> Boolean) (kotlin.properties)", typeText: "ReadWriteProperty<Any?, T>", attributes:"" }

// EXIST: createX1
// ABSENT: createX2
// EXIST: createX3
// EXIST: createY1
// ABSENT: createY2
// EXIST: createY3

// EXIST: X1
// ABSENT: X2
// EXIST: X3
// EXIST: Y1
// ABSENT: Y2
// ABSENT: Y3

// EXIST: { itemText:"X1.create", tailText:"() (<root>)", typeText:"X1", attributes:"" }
