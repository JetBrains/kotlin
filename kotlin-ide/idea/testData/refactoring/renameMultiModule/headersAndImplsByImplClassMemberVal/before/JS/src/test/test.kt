package test

actual class C {
    actual var /*rename*/foo: Int
        get() = 1
        set(value) {}

    actual var bar: Int
        get() = 1
        set(value) {}
}

fun test(c: C) {
    c.foo
    c.foo = 1
    bar
    bar = 1
}