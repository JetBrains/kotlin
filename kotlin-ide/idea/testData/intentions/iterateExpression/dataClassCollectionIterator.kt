// WITH_RUNTIME
data class Foo(val id: Int, val name: String)

fun test() {
    listOf(Foo(123, "def"), Foo(456, "abc"))<caret>
}