interface I {
    fun foo()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun bar(){}
    open val fromBase: String = ""
}

open class Base2 : Base1() {
}

class A(override val <caret>) : Base2(), I

// EXIST: { itemText: "override val someVal: Int", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override val fromBase: String", tailText: null, typeText: "Base1", attributes: "" }
// NOTHING_ELSE
