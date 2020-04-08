// OUT_OF_CODE_BLOCK: TRUE
// TYPE: '@Some("X")'

annotation class Some(val message: String)

class InPropertyAccessorWithAnnotation {
    val prop1: Int
        <caret>
        get() = 42
}