package ppp

class C {
    fun foo() {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "() for C in dependency", typeText: "Int" }
// NOTHING_ELSE
