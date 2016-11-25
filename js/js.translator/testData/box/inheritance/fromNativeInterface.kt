package foo

external interface A {
    val bar: Int get() = noImpl
    fun foo(): String = noImpl
}

class C : A

fun box(): String {
    val c = C()

    return "OK"
}
