private fun String.foo(): List<String> = emptyList()

fun test(s: String) {
    s.foo().flatMap { m ->
        m.foo().map { e ->
            e to m
        }
    }
}
