package capturedValues1

fun main(args: Array<String>) {
    1000.foo(args)
}

fun Int.foo(args: Array<String>) {
    val a = 1
    block {
        val b = 2
        val b2 = 2
        block("x") place1@ {
            val c = 3
            val c2 = 3
            block("y") place2@ {
                //Breakpoint!
                this@place1
                this@foo
                b
                c2
                args
            }
        }
    }
}

fun block(block: () -> Unit) {
    block()
}

fun <T> block(obj: T, block: T.() -> Unit) {
    obj.block()
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME