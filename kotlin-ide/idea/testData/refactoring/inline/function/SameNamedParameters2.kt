fun <caret>aaaaaa(b: Int) = bbbbbb(b = b) { bbbbbb(b = b) {} }
fun bbbbbb(b: Int, action: () -> Unit) {
    println(b)
    action()
}
fun test() {
    val variable = 1
    aaaaaa(variable)
}