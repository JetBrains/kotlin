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

    override fun <caret>
}

// EXIST: { itemText: "override fun bar() {...}", lookupString: "bar", tailText: null, typeText: "Base1", attributes: "" }
// EXIST: { itemText: "override fun foo() {...}", lookupString: "foo", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override fun equals(other: Any?): Boolean {...}", lookupString: "equals", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override fun hashCode(): Int {...}", lookupString: "hashCode", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override fun toString(): String {...}", lookupString: "toString", tailText: null, typeText: "Any", attributes: "" }
// NOTHING_ELSE
