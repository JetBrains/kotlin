// MODULE: main
// FILE: sample/VarargArrayClash.kt
package sample

// Same JVM shape (String[]); the ID keeps `vararg` and the array type apart.
fun h(vararg x: String) {}
fun h(x: Array<String>) {}
