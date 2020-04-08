// TYPE: 'z'
// OUT_OF_CODE_BLOCK: TRUE

data class Intz(val q: String)

class InSecondaryConstructor {
    init {
    }

    constructor(i: Int<caret>) {
    }
}
// SKIP_ANALYZE_CHECK