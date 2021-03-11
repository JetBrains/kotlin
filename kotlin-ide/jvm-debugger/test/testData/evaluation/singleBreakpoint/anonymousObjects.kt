package anonymousObjects

fun main(args: Array<String>) {
    val a = object: AbstractClass(1) {}
    a.test(1)
}

abstract class AbstractClass(val i: Int) {
    fun test(i: Int): Int {
        //Breakpoint!
        return i
    }
}

// EXPRESSION: test(2)
// RESULT: 2: I

// EXPRESSION: i
// RESULT: 1: I