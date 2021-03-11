interface I {
    fun someFun()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun bar(){}
}

open class Base2 : Base1() {
}

class A : Base2(), I {
    so<caret>
}

// ABSENT: { itemText: "override" }
// ABSENT: { itemText: "override fun bar() {...}" }
// EXIST: { itemText: "override fun someFun() {...}", lookupString: "override", allLookupStrings: "override, someFun", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override val someVal: Int", lookupString: "override", allLookupStrings: "override, someVal", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override var someVar: Int", lookupString: "override", allLookupStrings: "override, someVar", tailText: null, typeText: "I", attributes: "bold" }
