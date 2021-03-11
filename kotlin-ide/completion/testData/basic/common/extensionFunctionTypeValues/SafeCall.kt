fun test(i: Int?, foo: Int.(String) -> Char) {
    i?.fo<caret>
}

// EXIST: { lookupString: "foo", itemText: "foo", tailText: "(String)", typeText: "Char", attributes: "bold" }