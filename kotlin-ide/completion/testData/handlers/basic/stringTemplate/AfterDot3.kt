fun foo(param: String) {
    val s = "$param.eq<caret>bla-bla-bla"
}

// ELEMENT: equals
// TAIL_TEXT: "(other: Any?)"