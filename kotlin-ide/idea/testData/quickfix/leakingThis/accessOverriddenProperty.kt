// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ACTION: Convert property initializer to getter

open class My(open val x: Int) {
    val y = <caret>x
}

class Your(x : Int) : My(x) {
    override val x: Int get() = 42
}