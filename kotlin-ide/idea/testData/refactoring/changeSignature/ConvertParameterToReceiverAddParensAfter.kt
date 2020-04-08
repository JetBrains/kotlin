fun f(list: List<Any>) {
    list.filter { (it as String).foo() }
}

fun String.foo(): Boolean = true