// IS_APPLICABLE: false
// DISABLE-ERRORS
interface A {
    abstract fun <caret>foo(): Int
}

class B : A {

}