// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

interface T

fun test(): T = J.<caret>Foo(2, "2")