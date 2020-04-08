// IS_APPLICABLE: false
fun test() {
    class Test {
        operator fun <T> unaryPlus(): T? = this as? T
    }
    val test = Test()
    test.unaryP<caret>lus<Int>()
}
