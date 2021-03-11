// PROBLEM: none

class Test(val str: String) {
    private fun <caret>Test.print() {
        println(this@Test.str) // Unused receiver isn't reported because of explicit usage for parent `this`
    }
}

fun println(a: Any) {}