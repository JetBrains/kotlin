class Foo {
    val bar = 1
}

fun test(foo: Foo): Int {
    return <caret>when {
        foo.bar == 1 -> 10
        foo.bar == 2 -> 20
        else -> 30
    }
}