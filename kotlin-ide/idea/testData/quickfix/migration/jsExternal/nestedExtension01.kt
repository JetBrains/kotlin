// "Fix with 'asDynamic'" "false"
// JS
// ACTION: Convert to block body
// ACTION: Move to companion object
// ACTION: Remove explicit type specification
// ERROR: Declaration of such kind (extension function) can't be external

external class A {
    fun A.<caret>bar(): Nothing = definedExternally
}
