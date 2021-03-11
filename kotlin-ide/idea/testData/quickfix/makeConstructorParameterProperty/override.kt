// "class org.jetbrains.kotlin.idea.quickfix.MakeConstructorParameterPropertyFix" "false"
// ERROR: Cannot access 'foo': it is invisible (private in a supertype) in 'A'

open class Base(private val foo: String)

class A(foo: String) : Base(foo) {
    fun bar() {
        val a = foo<caret>
    }
}