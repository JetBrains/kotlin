// FLOW: OUT

class A(val <caret>n: Int) {
    val x = n

    val y: Int

    init {
        y = n

        bar(n)
    }

    fun bar(m: Int) {
        val z = n
    }
}

fun test() {
    val z = A(1).n
}