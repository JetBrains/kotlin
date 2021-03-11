package test

expect class C {
    var baz: Int
    var bar: Int
}

fun test(c: C) {
    c.baz
    c.baz = 1
    c.bar
    c.bar = 1
}