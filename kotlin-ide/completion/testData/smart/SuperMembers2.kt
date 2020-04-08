open class B {
    open fun foo(p1: Int, p2: String): Int = 0
    open fun bar(p1: Int, p2: String): Int = 0
}

class C : B() {
    override fun foo(p1: Int, p2: String): Int {
        return super.<caret>
    }
}

// EXIST: { lookupString: "foo", itemText: "foo", tailText: "(p1, p2)", typeText: "Int", attributes: "bold" }
// EXIST: { lookupString: "foo", itemText: "foo", tailText: "(p1: Int, p2: String)", typeText: "Int", attributes: "bold" }
// EXIST: { lookupString: "bar", itemText: "bar", tailText: "(p1: Int, p2: String)", typeText: "Int", attributes: "bold" }
// EXIST: hashCode
// NOTHING_ELSE
