fun test() {
    class Test {
        operator fun invoke(a: Int, b: String) {}
    }
    val test = Test()
    test.i<caret>nvoke(b = "s", a = 1)
}
