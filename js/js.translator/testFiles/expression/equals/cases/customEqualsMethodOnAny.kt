package foo

class Foo(val name: String) {
    public fun equals(that: Any?): Boolean {
        if (that !is Foo) {
            return false
        }
        return this.name == that.name
    }
}

fun callEqualsMethod(v1: Any?, v2: Any?): Boolean {
  return v1 == v2
}

fun box() : Boolean {
    val a = Foo("abc")
    val b = Foo("abc")
    val c = Foo("def")

    if (!callEqualsMethod(a, b)) return false
    if (callEqualsMethod(a, c)) return false
    return true
}