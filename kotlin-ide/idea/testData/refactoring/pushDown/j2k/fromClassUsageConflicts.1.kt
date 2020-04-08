class K : A()

fun test(a: A) {
    val t1 = a.x
    a.x = t1 + 1
    val t2 = A.X
    a.foo(1)
    A.foo2(2)
    A.Y()
}