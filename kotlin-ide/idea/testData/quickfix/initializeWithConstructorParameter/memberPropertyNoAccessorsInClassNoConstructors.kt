// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$InitializeWithConstructorParameter" "false"
// ERROR: Property must be initialized or be abstract
open class A {
    <caret>val n: Int
}

class B : A()

fun test() {
    val a = A()
}