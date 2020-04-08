fun foo(p: (String, Char) -> Unit){}

fun test() {
    foo<caret>
}

// EXIST: { lookupString:"foo", itemText: "foo", tailText: "(p: (String, Char) -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { lookupString:"foo", itemText: "foo", tailText: " { String, Char -> ... } (p: (String, Char) -> Unit) (<root>)", typeText:"Unit" }
