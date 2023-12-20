// MODULE: lib@name
// FILE: lib.kt

fun foo() = "OK"

// MODULE: main(lib@name)
// FILE: main.kt

fun box() = foo()