// TYPE: 'n.toString()'
// OUT_OF_CODE_BLOCK: FALSE

class InSecondaryConstructor(val name: String) {
    init {
    }

    constructor(n: Int): this(<caret>) {
    }
}