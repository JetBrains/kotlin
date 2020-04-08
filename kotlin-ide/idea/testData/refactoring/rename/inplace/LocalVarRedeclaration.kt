// SHOULD_FAIL_WITH: Variable 'localValB' is already declared in function 'containNames'
fun containNames() {
    val <caret>localValA = 11
    val localValB = 12
}