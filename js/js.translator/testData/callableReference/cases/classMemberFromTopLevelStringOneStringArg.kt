package foo

class A {
    fun foo(result: String) = result
}

fun box(): String {
    val x = A::foo
    return A().x("OK")
}
