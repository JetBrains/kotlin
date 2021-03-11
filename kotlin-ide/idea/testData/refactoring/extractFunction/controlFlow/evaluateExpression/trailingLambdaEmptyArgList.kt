fun <T> Array<T>.check(f: (T) -> Boolean): Boolean = false

// SIBLING:
fun foo(t: Array<Int>) {
    t.check() <selection>{ it + 1 > 1 }</selection>
}