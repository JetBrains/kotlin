// MODULE: main
// FILE: sample/ClashingCallables.kt
package sample

// Clashing callables: two properties with the same name (REDECLARATION, identical IDs — tolerated, the
// compiler rejects both), a function sharing the name with the property (no clash: the ID kind differs),
// and a genuine overload distinguished by its parameter list.
val c: Int = 1
val c: String = ""
fun c() {}
fun c(x: Int) {}
