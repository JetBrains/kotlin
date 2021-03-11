data class Foo(val a: String) {
    fun copy(i: Int) {}
}

class Foo2() {
    fun copy(i: Int) {}
}

fun bar(f: Foo, f2: Foo2) {
    f.copy("")
    f.copy()
    f.copy(a = "")
    f.copy(1)
    f2.copy(1)
}