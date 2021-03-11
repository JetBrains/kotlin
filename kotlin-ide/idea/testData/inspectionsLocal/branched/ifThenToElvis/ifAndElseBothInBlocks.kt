fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    val bar = "bar"
    if (foo != null<caret>) {
        foo
    }
    else {
        bar
    }
}
