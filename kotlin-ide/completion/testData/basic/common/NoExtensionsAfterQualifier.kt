// FIR_COMPARISON
package pack

fun String.extFoo(){}

fun foo() {
    pack.<caret>
}

// ABSENT: extFoo
