class C {
    inner class Inner {
        fun foo() {
            xx<caret>
        }

        val xxx: String get() = 1
    }

    val xxx: Int get() = 1
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "String" }
// NOTHING_ELSE
