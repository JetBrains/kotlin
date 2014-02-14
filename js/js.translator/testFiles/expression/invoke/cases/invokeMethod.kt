package foo

class Foo(val postfix: String) {
    public fun invoke(text: String): String {
        return text + postfix
    }
}

fun box(): String {
    val a = Foo(" world!")
    return a("hello")
}