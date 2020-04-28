fun f(list: List<Any>) {
    list.filter { foo(it as String) }
}

fun <caret>foo(s: String): Boolean = true