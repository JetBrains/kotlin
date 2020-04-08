import dependency.xxx

class C {
    fun xxx(){}
    fun xxy(){}
    fun xxz(p: Int){}

    fun f() {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "()", typeText: "Unit" }
// EXIST: { lookupString: "xxy", itemText: "xxy", tailText: "()", typeText: "Unit" }
// EXIST: { lookupString: "xxz", itemText: "xxz", tailText: "(p: Int)", typeText: "Unit" }
// EXIST: { lookupString: "xxz", itemText: "xxz", tailText: "() (dependency)", typeText: "Int" }
// NOTHING_ELSE
