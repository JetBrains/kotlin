// FLOW: OUT

class A(var <caret>n: Int) {
    val x = n

    val y: Int

    init {
        y = n

        bar(n)
    }

    fun bar(m: Int) {
        val z = n
        n = 1
    }
}

fun test() {
    val z = A(1).n
    A(1).n = 2
}