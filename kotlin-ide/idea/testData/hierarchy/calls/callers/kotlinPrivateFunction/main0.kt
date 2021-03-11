open class Tester {
    open fun test() {
        privateTest()
    }

    private fun <caret>privateTest() {
        println(this)
    }
}