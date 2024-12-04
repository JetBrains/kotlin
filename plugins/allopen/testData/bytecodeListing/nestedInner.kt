annotation class AllOpen

@AllOpen
class Test {
    fun testMethod() {}

    class Nested {
        fun nestedMethod() {}
    }

    inner class Inner {
        fun innerMethod() {}
    }
}