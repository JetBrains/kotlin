// FLOW: IN

class A {
    var <caret>x: Int

    init {
        x = 1
    }

    val y = x

    fun test() {
        val y = x
        x = 2
    }
}