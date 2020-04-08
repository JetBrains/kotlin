// NAME: A
// SIBLING:
class <caret>Foo {
    // INFO: {checked: "true"}
    private fun privateFun() = 0

    fun refer() = privateFun()
}