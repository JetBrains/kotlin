fun foo(s: String) {
    if (s in <caret>)
}

// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg elements: String) (kotlin.collections)", typeText:"List<String>" }
