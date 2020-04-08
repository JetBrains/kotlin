// INTENTION_TEXT: Replace with '-' operator
fun test() {
    class Test {
        operator fun unaryMinus(): Test = Test()
    }
    val test = Test()
    test.unaryMin<caret>us()
}
