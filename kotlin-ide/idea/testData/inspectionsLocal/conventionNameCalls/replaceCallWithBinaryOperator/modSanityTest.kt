// PROBLEM: none
// LANGUAGE_VERSION: 1.2

fun test() {
    class Test {
        operator fun mod(a: Int): Test = Test()
    }
    val test = Test()
    test.<caret>mod(1)
}
