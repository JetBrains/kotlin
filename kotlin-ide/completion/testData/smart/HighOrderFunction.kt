class C {
    companion object {
        fun foo(p: (Int) -> Unit) = C()
    }
}

fun foo(p: (String, Char) -> Unit): C {}

val handler1: (String, Char) -> Unit = {}
val handler2: (Int) -> Unit = {}

fun v: C = fo<caret>

// EXIST: { lookupString:"foo", itemText: "foo", tailText: "(p: (String, Char) -> Unit) (<root>)", typeText:"C" }
// EXIST: { lookupString:"foo", itemText: "foo", tailText: " { String, Char -> ... } (p: (String, Char) -> Unit) (<root>)", typeText:"C" }
// EXIST: { lookupString:"foo", itemText: "foo", tailText: "(handler1) (<root>)", typeText:"C" }
// EXIST: { allLookupStrings: "C, foo", itemText: "C.foo", tailText: " {...} (p: (Int) -> Unit) (<root>)", typeText:"C" }
// EXIST: { allLookupStrings: "C, foo", itemText: "C.foo", tailText: "(handler2) (<root>)", typeText:"C" }
