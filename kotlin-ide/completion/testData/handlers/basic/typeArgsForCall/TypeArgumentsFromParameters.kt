fun <T1, T2> T1.foo(t: T2): T2 = t

fun foo() {
    "".<caret>
}

// ELEMENT: foo
