// "Create parameter 'foo'" "true"
// ACTION: Create local variable 'foo'

fun test(n: Int) {
    val f: () -> Int = { <caret>foo }
}