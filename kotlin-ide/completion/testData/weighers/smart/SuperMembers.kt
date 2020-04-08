class B {
    open fun foo1(): String = ""
    open fun foo2(): String = ""
    open fun foo3(): String = ""
}

class C : B() {
    override fun foo2(): String {
        return super.fo<caret>
    }
}

// ORDER: foo2
// ORDER: foo1
// ORDER: foo3