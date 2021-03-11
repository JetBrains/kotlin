// PROBLEM: none
// DISABLE-ERRORS

enum class E(val value: String) {
    E1(<caret>Companion.foo);

    companion object {
        const val foo = ""
    }
}