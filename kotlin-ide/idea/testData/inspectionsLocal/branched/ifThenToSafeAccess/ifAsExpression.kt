fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    val foo = maybeFoo()
    val x = <caret>if (foo == null) {
        null
    }
    else {
        foo.length
    }
}
