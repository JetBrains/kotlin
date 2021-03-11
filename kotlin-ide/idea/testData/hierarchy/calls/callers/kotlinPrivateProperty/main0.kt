open class Tester {
    open fun test() {
        privateTest = "test"
        println(privateTest)
    }

    private var <caret>privateTest: String
        get() = ""
        set(value) { println(value) }
}