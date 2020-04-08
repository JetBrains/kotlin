package ppp

val fooGlobal: Int.() -> Unit = { }

val String.fooExt: Int.() -> Unit get() = { }

fun String.test(i: Int, foo: Int.(String) -> Char, fooAny: Any.() -> Unit) {
    i.fo<caret>
}

// EXIST: { lookupString: "foo", itemText: "foo", tailText: "(String)", typeText: "Char", attributes: "bold" }
// EXIST: { lookupString: "fooAny", itemText: "fooAny", tailText: "()", typeText: "Unit", attributes: "" }
// EXIST: { lookupString: "fooGlobal", itemText: "fooGlobal", tailText: "() (ppp)", typeText: "Unit", attributes: "bold" }
// ABSENT: fooExt