// !IGNORE_FIR

inline fun <reified T : Any> foo(init: T.() -> Unit = {}): T {
    TODO("message")
}

inline fun <T : Any> bar(init: T.() -> Unit = {}): T {
    TODO("message")
}

fun resolve() {
    foo<String>()
    val x: String = foo()

    bar<String>()
    val y: String = bar()

    val z = listOf("foo").filterIsInstance<String>()
}
