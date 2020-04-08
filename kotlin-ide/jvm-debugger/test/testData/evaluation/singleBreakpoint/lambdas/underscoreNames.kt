package underscoreNames
data class A(val x: Double = 1.0, val y: String = "", val z: Char = '0')

fun foo(a: A, block: (A, String, Int) -> String): String = block(a, "", 1)

val arrayOfA: Array<A> = Array(1) { A() }

fun main(args: Array<String>) {

    foo(A()) {
        (x, _, y), _, w ->

        val (a, _, c) = A()
        val (_, `_`, d) = A()

        for ((_, q) in arrayOfA) {
            //Breakpoint! (lambdaOrdinal = 1)
            println(q)
        }

        ""
    }
}

// PRINT_FRAME

// EXPRESSION: x
// RESULT: 1.0: D

// EXPRESSION: y
// RESULT: 48: C

