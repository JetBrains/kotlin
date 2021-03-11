// NAME: I

// SIBLING:
class <caret>PrivateRef {
    // INFO: {checked: "true", toAbstract: "false"}
    private fun privateFun() = 0
    // INFO: {checked: "true", toAbstract: "true"}
    fun refer() = privateFun()
}