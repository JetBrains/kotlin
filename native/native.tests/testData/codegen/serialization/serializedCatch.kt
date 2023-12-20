// MODULE: lib
// FILE: lib.kt

inline fun foo(): String {
    try {
        try {
            throw Exception("O")
        } catch (e: Throwable) {
            throw Exception("${e.message}K")
        }
    } catch (e: Throwable) {
        return e.message!!
    }
    return "FAIL"
}

// MODULE: main(lib)
// FILE: main.kt

fun box() = foo()