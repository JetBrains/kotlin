package initializer

fun main(args: Array<String>) {
    A(1)
}

class A(val prop: Int) {
    init {
        // EXPRESSION: prop
        // RESULT: 1: I
        //Breakpoint!
        val a = 1

        val list = listOf(1)
        // EXPRESSION: it
        // RESULT: Unresolved reference: it
        //Breakpoint! (lambdaOrdinal = -1)
        list.map { it * 2 }
    }
}
