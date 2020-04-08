// FLOW: OUT

class A(<caret>n: Int) {
    val x = n

    val y: Int

    init {
        y = n

        bar(n)
    }

    fun bar(m: Int) {

    }
}