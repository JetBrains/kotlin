package test

expect var baz: Int
expect var bar: Int

fun test() {
    baz
    baz = 1
    bar
    bar = 1
}