inline fun <reified R> Iterable<*>.myFilterIsInstance(): List<R> {
    return filterIsInstanceTo(ArrayList<R>())
}

fun foo(list: List<Any>) {
    list.<caret>filter<String>(x)
}

// ELEMENT: myFilterIsInstance
// CHAR: '\t'
