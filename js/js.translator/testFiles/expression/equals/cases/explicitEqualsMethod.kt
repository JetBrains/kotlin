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

fun box(): Boolean {
    val a = Foo("abc")
    val b = Foo("abc")
    val c = Foo("def")

    if (!(a equals b)) return false
    if (a equals c) return false
    if (Bar() equals Bar()) return false
    val g = Bar()
    if (!(g equals g)) return false
    if (g equals Bar()) return false
    return true
}