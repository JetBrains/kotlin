class A {
    @Deprecated("") // it is deprecated
    public override fun e<caret>
}

// ELEMENT_TEXT: "override fun equals(other: Any?): Boolean {...}"
