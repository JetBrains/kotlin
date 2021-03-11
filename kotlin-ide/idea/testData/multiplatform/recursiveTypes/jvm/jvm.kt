package sample

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>A<!><T : A<T>> {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>foo<!>(): T
    fun bar() : T
}

fun test_1(a: A<*>) {
    a.foo()
    a.bar()
    a.foo().foo()
    a.bar().bar()
    a.foo().bar()
    a.bar().foo()
}

fun test_2(b: B) {
    b.foo()
    b.bar()
    b.foo().foo()
    b.bar().bar()
    b.foo().bar()
    b.bar().foo()
}