class Declarations {
    val a: String = "a"
    var b: String
        get() = "A"
        set(v) { println(v) }
    val c: String

    class NestedClass {
        val b: String = "b"
    }
    inner class InnerClass {
        val c: CharSequence = a
    }

    companion object {
        val CONST_VAL = 1
    }

    companion object A {
        fun b(): Boolean = true
    }

    fun func(a: Int, b: String): Int {
        return (a + 1) * b.length
    }
}