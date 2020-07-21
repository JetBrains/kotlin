// WITH_RUNTIME

fun doo(s: String): String = "42"

fun Int.foo(body: (String) -> String) = Unit

fun main() {
    42.foo(::doo<caret>)
}