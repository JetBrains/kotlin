package foo

open class A(val name: String) {
    fun foo(): String {
        return name
    }
}

object B : A("OK")

fun box(): String {
    val OK = B.foo()
    return OK
}