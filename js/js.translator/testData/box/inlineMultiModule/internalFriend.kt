// MODULE: lib
// FILE: lib.kt
internal fun bar() = "OK"

internal inline fun foo() = bar()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String = foo()
