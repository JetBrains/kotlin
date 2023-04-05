// NO_COMMON_FILES
// MODULE: lib
// FILE: lib.kt

private fun ok() = "Lib"

fun testLib() = ok()

// MODULE: main(lib)
// FILE: mainAux.kt
// RECOMPILE

private fun ok() = "Aux"

fun testAux() = ok()

// FILE: main.kt

private fun ok() = "OK"

fun box(): String {
    if (testLib() != "Lib") return "fail1"
    if (testAux() != "Aux") return "fail2"
    return ok()
}
