// "Remove variable 'a'" "true"
fun test() {
    val <caret>a: (String) -> Unit = { s: String -> s + s }
}