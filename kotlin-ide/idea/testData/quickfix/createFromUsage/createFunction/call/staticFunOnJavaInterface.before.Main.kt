// "Create function 'foo'" "false"
// "Create member function 'foo'" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo("1", 2)
}
