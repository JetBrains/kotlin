fun f() {
    loop@ while (true) {
        break@lo<caret>op
    }
}
// KT-4515: Extract variable can attempt to extract a label from a labelled statement.
/* */
