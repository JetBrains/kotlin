// WITH_RUNTIME
fun test() {
    "ABC".equa<caret>ls(Foo().bar(), ignoreCase = true)
}

class Foo {
    fun bar(): String = ""
}
