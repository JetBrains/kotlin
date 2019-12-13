// "Surround with null check" "true"
fun test(a: String, b: List<String>?) {
    a <caret>in b
}