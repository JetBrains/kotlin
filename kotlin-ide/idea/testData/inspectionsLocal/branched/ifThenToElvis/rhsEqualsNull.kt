fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val bar = "bar"
    val foo = maybeFoo()
    if (null == foo<caret>)
        bar
    else
        foo
}
