fun box(): String {
    val x = FooImpl()
    return x.foo() + x.bar()
}