class A {
    operator fun get(i: Int) {}
}

fun usage(a: A) {
    a[42]
    A()[42]
    a.get(42)
}