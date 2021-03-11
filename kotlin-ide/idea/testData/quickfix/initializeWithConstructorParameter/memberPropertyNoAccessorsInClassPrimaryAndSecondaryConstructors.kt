// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$InitializeWithConstructorParameter" "false"
// ERROR: Property must be initialized or be abstract
open class A(s: String) {
    <caret>val n: Int

    constructor(): this("")
    constructor(a: Int): this("" + a)
}

class B : A("")

fun test() {
    val a = A("")
}