val xxx: Int get() = 1

class C {
    fun foo() {
        xx<caret>
    }

    val xxx: String get() = 1
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "String" }
// NOTHING_ELSE
