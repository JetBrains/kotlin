// IS_APPLICABLE: false
// ERROR: 'operator' modifier is inapplicable on this function: must have no value parameters
fun test() {
    class Test {
        operator fun unaryPlus(a: Int): Test = Test()
    }
    val test = Test()
    test.unaryPl<caret>us(1)
}
