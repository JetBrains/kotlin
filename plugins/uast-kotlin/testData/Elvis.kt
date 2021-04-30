// !IGNORE_FIR

fun foo(bar: String): String? = null

fun bar() = 42

fun baz(): String? {
    return foo("Lorem ipsum") ?: foo("dolor sit amet") ?: bar().toString()
}
