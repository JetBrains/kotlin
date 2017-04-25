// EXPECTED_REACHABLE_NODES: 492
package foo

class Foo(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (other !is Foo) {
            return false
        }
        return this.name == other.name
    }
}

fun callEqualsMethod(v1: Any?, v2: Any?): Boolean {
    return v1 == v2
}

fun box(): String {
    val a = Foo("abc")
    val b = Foo("abc")
    val c = Foo("def")

    if (!callEqualsMethod(a, b)) return "fail1"
    if (callEqualsMethod(a, c)) return "fail2"
    return "OK"
}