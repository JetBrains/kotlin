annotation class AllOpen

@AllOpen
class Test {
    val prop: String = ""
    fun method() {}

    class Nested {
        fun nestedMethod() {}
    }
}
