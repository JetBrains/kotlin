package foo

import js.*

native
class A(b: Int) {
    fun g(): Int = js.noImpl
    fun m(): Int = js.noImpl
}


fun box(): Boolean {
    if (A(2).g() != 4) {
        return false;
    }
    if (A(3).m() != 2) {
        return false;
    }
    val a = A(100)
    if (a.g() != 200) {
        return false;
    }
    if (a.m() != 99) {
        return false;
    }
    return true;
}