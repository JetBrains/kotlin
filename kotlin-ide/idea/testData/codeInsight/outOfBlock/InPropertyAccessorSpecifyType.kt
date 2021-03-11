// OUT_OF_CODE_BLOCK: TRUE
// TYPE: 'Int'

class InPropertyAccessorSpecifyType {
    val prop1: Int
        get():<caret> = 42
}

// TODO: Investigate
// SKIP_ANALYZE_CHECK
