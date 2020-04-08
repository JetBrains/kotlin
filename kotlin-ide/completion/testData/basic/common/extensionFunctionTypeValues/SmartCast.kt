interface I
interface J

fun test(p: I, fooI: I.() -> Unit, fooJ: J.() -> Unit) {
    if (p is J) {
        p.fo<caret>
    }
}

// EXIST: { lookupString: "fooI", itemText: "fooI", tailText: "()", typeText: "Unit", attributes: "bold" }
// EXIST: { lookupString: "fooJ", itemText: "fooJ", tailText: "()", typeText: "Unit", attributes: "bold" }