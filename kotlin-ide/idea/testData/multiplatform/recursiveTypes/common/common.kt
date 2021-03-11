package sample

expect interface <!LINE_MARKER("descr='Is subclassed by B'"), LINE_MARKER("descr='Has actuals in JVM'")!>A<!><T : A<T>> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>foo<!>(): T
}

interface B : A<B>

fun test(a: A<*>) {
    a.foo()
    a.foo().foo()
}

fun test(b: B) {
    b.foo()
    b.foo().foo()
}
