fun <caret>aaaaaa(a: Int) = bbbbbb(b = a)
fun bbbbbb(b: Int) = b
fun test() {
    val variable = 1
    aaaaaa(variable)
}