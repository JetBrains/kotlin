fun <caret>aaaaaa(b: Int) = bbbbbb(b = b)
fun bbbbbb(b: Int) = b
fun test() {
    val variable = 1
    aaaaaa(variable)
}