package foo

import js.*

native
class A(val c: Int) {
    native
    class object {
        val g: Int = js.noImpl
        val c: String = js.noImpl
    }
}

fun box(): Boolean {
    if (A.g != 3) return false
    if (A.c != "hoooray") return false
    if (A(2).c != 2) return false

    return true
}