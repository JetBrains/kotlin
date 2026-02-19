// NO_COMMON_FILES
// MODULE: lib
// FILE: lib.kt

fun foo() = "OK"

// MODULE: main(lib)
// FILE: mainAux.kt

inline fun proxy() = foo()

// FILE: mainDummy.kt
// RECOMPILE

val dummy get() = js("\$module\$lib")

// FILE: main.kt

fun box() = proxy()
