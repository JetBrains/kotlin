package foo

class A

fun A.foo() = "OK"

fun box(): String {
    val x = A::foo
    return A().x()
}
