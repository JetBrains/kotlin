interface I {
    val p: Int
}

class CCCC(over<caret>val x: Int) : I

// ELEMENT_TEXT: "override val p: Int"
