interface I {
    val someVal: String?
}

class A : I {
    override val <caret>
}

// ELEMENT_TEXT: "override val someVal: String?"
