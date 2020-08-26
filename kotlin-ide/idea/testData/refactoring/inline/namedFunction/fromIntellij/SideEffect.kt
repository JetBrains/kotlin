class Test {
    private var s: String? = null

    fun <caret>method(): String {
        s = "Hello"
        return s
    }

    fun test() {
        println(method())
        println(s)
    }
}