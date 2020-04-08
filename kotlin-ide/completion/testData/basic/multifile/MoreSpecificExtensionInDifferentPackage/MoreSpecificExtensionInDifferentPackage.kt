package ppp

class C {
    fun foo() {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "() for Any in dependency1", typeText: "Int" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "() for C in dependency2", typeText: "Int" }
// NOTHING_ELSE
