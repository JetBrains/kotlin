// PROBLEM: none

class My(val x: Int = IMPORTANT_CONST) {
    companion object {
        val <caret>IMPORTANT_CONST = 42
    }
}