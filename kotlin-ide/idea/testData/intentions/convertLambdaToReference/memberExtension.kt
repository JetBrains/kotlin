// IS_APPLICABLE: false

class Their {
    fun Int.foo() = "x"

    val x = { arg: Int -> <caret>arg.foo() }
}