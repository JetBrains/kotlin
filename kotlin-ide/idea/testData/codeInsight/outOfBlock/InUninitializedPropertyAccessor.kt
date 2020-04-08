// OUT_OF_CODE_BLOCK: TRUE
// TYPE: '//'
// ERROR: Property must be initialized

var prop1: Int
    set(value) {
        <caret> println("prop.setter")
        field = value
    }

// TODO: Investigate
// SKIP_ANALYZE_CHECK
