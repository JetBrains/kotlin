open class Base {
    open fun foo(p1: Int, vararg p2: Int){}
}

class Derived : Base() {
    override fun foo(p1: Int, vararg p2: Int) {
        super.<caret>
    }
}

// ELEMENT: foo
// TAIL_TEXT: "(p1, *p2)"