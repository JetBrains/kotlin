package foo

external class A(x: Int) {
    var x: Int
        get() = noImpl
        set(value) = noImpl

    fun foo(): Int = noImpl

    class B(val value: Int) {
        fun bar(): Int = noImpl
    }
}

fun box(): String {
    var b = A.B(23)
    if (b.bar() != 10023) return "failed1: ${b.bar()}"

    return "OK"
}

