fun test() {
    class Test()

    operator fun Test.get(i: Int) : Int = 0

    val test = Test()
    test.g<caret>et(0)
}
