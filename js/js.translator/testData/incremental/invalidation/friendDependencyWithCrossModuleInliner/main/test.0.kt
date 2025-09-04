fun test(): Int {
    val v = PublicClassHeir()
    return v.foo() + v.bar + v.baz() - 5
}
