// FIR_COMPARISON
package foo

object A

fun A.foo() {}

fun some() {
    foo.A.<caret>
}

// EXIST: foo
