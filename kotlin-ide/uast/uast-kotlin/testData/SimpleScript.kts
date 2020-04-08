println("Hello World!")

fun getBarOrNull(flag: Boolean): Bar? {
    return if (flag) Bar(42) else null
}

class Bar(val a: Int) {
    val b: Int = 0

    fun getAPlusB() = a + b

    class Baz {
        fun doSomething() {

        }
    }
}

getBarOrNull(true)
println("Goodbye World!")
