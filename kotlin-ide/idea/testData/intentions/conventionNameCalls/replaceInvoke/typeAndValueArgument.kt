// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun <T> invoke(a: Int) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int>(0)
}
