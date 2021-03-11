fun test() {
    A().foo()
    B().foo()

    val a = A::foo
    val b = B::foo
}