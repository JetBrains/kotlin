// EXPECTED_REACHABLE_NODES: 493
package foo

class Foo(val name: String) {
    override fun equals(other: Any?): Boolean {
        if (other !is Foo) {
            return false
        }
        return this.name == other.name
    }
}

class Bar() {

}

fun box(): String {
    val a = Foo("abc")
    val b = Foo("abc")
    val c = Foo("def")

    if (!(a.equals(b))) return "fail1"
    if (a.equals(c)) return "fail2"
    if (Bar().equals(Bar())) return "fail3"
    val g = Bar()
    if (!(g.equals(g))) return "fail4"
    if (g.equals(Bar())) return "fail5"
    return "OK"
}