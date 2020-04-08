package testing

class Some : SomeClass() {
    val test = SomeClass()

    fun testFun(param : SomeClass) : SomeClass {
        return test;
    }
}