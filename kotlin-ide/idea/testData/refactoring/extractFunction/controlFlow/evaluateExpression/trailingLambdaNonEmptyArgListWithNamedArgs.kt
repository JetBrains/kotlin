fun <T> Array<T>.check(a: Int, b: Int, f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Int>) {
    t.check(a = 1, b = 2) <selection>{ it + 1 > 1 }</selection>
}