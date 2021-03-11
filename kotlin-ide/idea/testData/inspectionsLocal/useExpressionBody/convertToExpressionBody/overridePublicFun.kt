open class A {
    public open fun foo(): String = ""
}

class B : A() {
    public override fun foo(): String {
        <caret>return "abc"
    }
}
