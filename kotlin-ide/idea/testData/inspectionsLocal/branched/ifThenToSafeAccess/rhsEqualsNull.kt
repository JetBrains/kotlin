fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    i<caret>f (null == foo)
        null
    else
        foo.length
}
