public inline fun <reified T> Iterable<*>.myFirstIsInstance(): T { }

fun foo(list: List<Any>): String {
    return list.<caret>
}

// ELEMENT: myFirstIsInstance
