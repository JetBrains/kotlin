package foo

class Foo(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (other !is Foo) {
            return false
        }
        return this.name == other.name
    }
}

fun callEqualsMethod(v1: Foo?, v2: Foo?): Boolean {
    return v1 == v2
}

fun box(): Boolean {
    val a = Foo("abc")
    val b = Foo("abc")
    val c = Foo("def")

    if (!callEqualsMethod(a, b)) return false
    if (callEqualsMethod(a, c)) return false
    return true
}