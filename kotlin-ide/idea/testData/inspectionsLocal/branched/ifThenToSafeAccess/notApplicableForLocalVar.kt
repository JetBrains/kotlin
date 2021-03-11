// PROBLEM: none
fun maybeFoo(): String? {
    return "foo"
}

fun main(args: Array<String>) {
    var foo = maybeFoo()
    if (foo == null<caret>)
        null
    else
        foo?.length
}
