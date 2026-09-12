package foo

fun useA(a: A) {
    a.f()


    fun A.bar(): String {
        return "bar"
    }

    foo.A().bar()
    Unit
}