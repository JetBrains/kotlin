package foo

open class A {
    @native class B(value: Int = 0) {
        val foo: Int
            get() = noImpl
        fun bar(): Int = noImpl
    }
}

fun box(): String {
    var b = A.B(23)
    if (b.foo != 123) return "failed1: ${b.foo}"
    if (b.bar() != 1123) return "failed2: ${b.bar()}"
    return "OK"
}
