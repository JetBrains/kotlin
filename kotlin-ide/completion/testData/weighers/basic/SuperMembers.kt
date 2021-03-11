class B {
    open fun foo1(): String = ""
    open fun foo2(): String = ""
    open fun foo3(): Int = ""
}

class C : B() {
    override fun foo2(): String {
        takeInt(super.fo<caret>)
    }
}

fun takeInt(p: Int){}

// ORDER: foo2
// ORDER: foo3
// ORDER: foo1
