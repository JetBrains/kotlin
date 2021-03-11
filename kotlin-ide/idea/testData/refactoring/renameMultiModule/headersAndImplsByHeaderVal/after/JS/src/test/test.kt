package test

actual var baz: Int
    get() = 1
    set(value) {}

actual var bar: Int
    get() = 1
    set(value) {}

fun test() {
    baz
    baz = 1
    bar
    bar = 1
}