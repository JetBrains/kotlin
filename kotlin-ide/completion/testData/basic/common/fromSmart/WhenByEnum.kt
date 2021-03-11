enum class E {
    A,
    B,
    C
}

fun foo(e: E) {
    when (e) {
        E.A -> {}

        <caret>
    }
}

// EXIST: { lookupString:"B", itemText:"E.B", tailText:" (<root>)", typeText:"E" }
// EXIST: { lookupString:"C", itemText:"E.C", tailText:" (<root>)", typeText:"E" }
// ABSENT: A
