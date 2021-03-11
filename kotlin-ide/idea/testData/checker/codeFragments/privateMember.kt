fun foo() {
    <caret>val a = 1
}

class MyClass {
    private fun privateFun() = 1
    private val privateVal = 1

    private class PrivateClass {
        val a = 1
    }
}