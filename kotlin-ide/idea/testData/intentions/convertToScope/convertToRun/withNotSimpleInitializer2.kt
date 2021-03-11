// WITH_RUNTIME
// IS_APPLICABLE: false

class MyClass {
    fun foo() {
        val c = 2
        (c.div(2) + 3)<caret>
        c.div(c + 2 + c) + c.div(c)
    }
}