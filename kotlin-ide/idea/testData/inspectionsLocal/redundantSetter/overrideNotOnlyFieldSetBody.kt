// PROBLEM: none
interface A {
    var myVar: Boolean
}

class X : A {
    override var myVar: Boolean = false
        <caret>set(value) {
        foo()
        field = value
    }
    fun foo() {}
}