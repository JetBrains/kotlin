package foo

data class A(val a: Foo<String>) {}

class Foo<T>(val a: T) { }

fun box() : Boolean {
    val f1 = Foo("a")
    val f2 = Foo("b")
    val a = A(f1)
    val b = a.copy(f2)
    if (b.a.a == "b") {
        return true
    }
    return false
}
