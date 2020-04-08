// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo() {
    val bar = { x: Int -> x + 1 }
    val incremented = listOf(1, 2, 3).map<caret>(bar)
}