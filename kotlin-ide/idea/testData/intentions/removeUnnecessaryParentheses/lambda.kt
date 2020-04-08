// IS_APPLICABLE: false

fun main() {
    foo()
    <caret>({ foo() } )
}

fun foo() {}
