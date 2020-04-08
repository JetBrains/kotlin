// NAME: I
// SIBLING:
class <caret>BrokenRef {
    private fun fun1() {}
    fun fun2() {}

    // INFO: {checked: "true", toAbstract: "true"}
    fun refer() {
        fun1()
        fun2()
    }
}