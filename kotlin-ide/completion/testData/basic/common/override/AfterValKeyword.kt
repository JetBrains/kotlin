interface I {
    fun foo()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun bar(){}
}

open class Base2 : Base1() {
}

class A : Base2(), I {
    fun x() {
        unresolved1(unresolved2)
    }

    override val <caret>
}

// EXIST: { itemText: "override val someVal: Int", lookupString: "someVal", tailText: null, typeText: "I", attributes: "bold" }
// NOTHING_ELSE
