// SHOULD_FAIL_WITH: Function 'localFunB' is already declared in function 'containNames'
fun containNames() {
    fun <caret>localFunA() = 11
    fun localFunB() = 12
}