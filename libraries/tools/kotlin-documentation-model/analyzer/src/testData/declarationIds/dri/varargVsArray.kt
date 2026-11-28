// MODULE: main
// FILE: test/VarargVsArray.kt
package test

// From DRITest.#4049: vararg and Array must get distinct IDs.
fun f1(vararg s: String) {}
fun f2(s: Array<String>) {}
