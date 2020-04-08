class C {
    val xxx = 1

    fun foo(xxx: String) {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "String" }
// NOTHING_ELSE
