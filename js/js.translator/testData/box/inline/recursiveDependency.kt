// IGNORE_BACKEND: JS
// MODULE: lib
// FILE: A.kt
inline fun funA(flag: Boolean): String {
    if (flag) {
        return funB(flag)
    }
    return "OK"
}

// FILE: B.kt
inline fun funB(flag: Boolean): String {
    val f = ::funA
    if (flag) {
        return f(false)
    }
    return "NOT OK"
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return funA(true)
}
