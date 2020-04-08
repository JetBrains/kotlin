fun foo(param: String) {
    val s = "$<caret>bla-bla-bla"
}

// EXIST: foo
// EXIST: param
