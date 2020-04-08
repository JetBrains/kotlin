enum class E {
    A,
    B
}

fun foo(): E {
    return <caret>
}

// EXIST: { lookupString:"A", itemText:"E.A", tailText:" (<root>)", typeText:"E" }
// EXIST: { lookupString:"B", itemText:"E.B", tailText:" (<root>)", typeText:"E" }
