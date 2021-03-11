// "Replace with 'new'" "true"
// WITH_RUNTIME

class A {
    @Deprecated("msg", ReplaceWith("new"))
    var old
        get() = new
        set(value) {
            new = value
        }

    var new = ""
}

fun foo() {
    val a = A()
    a.apply {
        <caret>old = "foo"
    }
}