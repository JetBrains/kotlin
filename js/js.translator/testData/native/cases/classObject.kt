package foo

native
class A(val c: Int) {
    native
    default object {
        val g: Int = noImpl
        val c: String = noImpl
    }
}

fun box(): Boolean {
    if (A.g != 3) return false
    if (A.c != "hoooray") return false
    if (A(2).c != 2) return false

    return true
}
