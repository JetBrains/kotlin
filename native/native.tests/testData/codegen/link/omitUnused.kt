// MODULE: lib
// FILE: lib.kt

package foo

fun foo() {}

// MODULE: main(lib)
// FILE: main.kt

// TODO: This test should be checking that there's no foo in the resulting binary.

fun box(): String = "OK"