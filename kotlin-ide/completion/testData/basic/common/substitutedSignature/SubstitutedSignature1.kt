fun foo(list: MutableList<String>) {
    list.<caret>
}

// EXIST: { itemText: "add", tailText: "(element: String)", typeText: "Boolean" }
// EXIST: { itemText: "iterator", tailText: "()", typeText: "Iterator<String>" }
