class TestFromJava() : BaseJava() {
    override fun testMethod() {
    }
}

fun test() {
    BaseJava().testMethod<caret>()
}

// REF: (in TestFromJava).testMethod()
// REF: (in BaseJava).testMethod()
