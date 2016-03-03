class Declarations {
    val a: String = "a"
    class NestedClass {
        val b: String = "b"
    }
    inner class InnerClass {
        val c: CharSequence = a
    }

    fun func(a: Int, b: String): Int {
        return (a + 1) * b.length
    }
}