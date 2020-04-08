fun test(i: Int, foo1: Int.(String) -> Char, foo2: Int.() -> Int, foo3: String.() -> Char): Char {
    return i.<caret>
}

// EXIST: { lookupString: "foo1", itemText: "foo1", tailText: "(String)", typeText: "Char", attributes: "bold" }
// ABSENT: foo2
// ABSENT: foo3
