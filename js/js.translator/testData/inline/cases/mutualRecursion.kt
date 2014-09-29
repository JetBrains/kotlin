package foo

inline fun f() { g() }

inline fun g() { h() }

inline fun h() { f() }

fun box(): String {
    f()

    return "OK"
}