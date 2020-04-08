interface A {
    fun foo(<caret>s: String, vararg args: Any)
}
class B : A {
    override fun foo(s: String, vararg args: Any) {
    }
}