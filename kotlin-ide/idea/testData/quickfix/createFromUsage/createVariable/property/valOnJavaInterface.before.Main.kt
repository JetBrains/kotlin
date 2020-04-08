// "Create property 'foo'" "false"
// ACTION: Create extension property 'A.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

internal fun test(a: A): String? {
    return a.<caret>foo
}