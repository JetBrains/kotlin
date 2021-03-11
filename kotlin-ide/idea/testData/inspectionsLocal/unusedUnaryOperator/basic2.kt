// "Remove unused unary operator" "true"
fun test(foo: Int?): Int {
    val a = 1 + 2
    - 3<caret>
    return a
}