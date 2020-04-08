import kotlin.properties.*

fun f(readonlypr<caret>)

// EXIST: { itemText: "readOnlyProperty: ReadOnlyProperty", tailText: "<T, V> (kotlin.properties)" }
// NUMBER: 1
