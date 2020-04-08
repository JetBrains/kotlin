// SHOULD_FAIL_WITH: Class 'LocalClassB' is already declared in function 'containNames'
fun containNames() {
    class <caret>LocalClassA {}
    class LocalClassB {}
}