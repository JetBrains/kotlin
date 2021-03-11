public inline fun <reified T> Iterable<*>.myFilterIsInstance(): Collection<T> { }

fun foo(list: List<Any>): String {
    return list.<caret>
}

// ELEMENT: myFilterIsInstance
