// "class org.jetbrains.kotlin.idea.quickfix.CastExpressionFix" "false"
// ERROR: Type mismatch: inferred type is A but B was expected
open class A
class B : A()

fun foo(a: A): B {
    return a: A<caret>
}
