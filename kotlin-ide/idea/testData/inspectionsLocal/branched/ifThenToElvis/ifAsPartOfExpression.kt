fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    val bar = "bar"
    val x = "baz" + if (foo == null<caret>) bar else foo
}
