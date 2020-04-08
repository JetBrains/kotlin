// "Create extension function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int? {
    return A().<caret>foo(1, "2")
}