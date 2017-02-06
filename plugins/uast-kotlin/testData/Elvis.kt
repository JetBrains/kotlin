
fun foo(bar: String): Any? = null

fun baz(): Any? {
    return foo("Lorem ipsum") ?: foo("dolor sit amet") ?: foo("consectetuer adipiscing elit")
}