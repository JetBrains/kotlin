package capturedValues2

fun main() {
    val a = 1

    block {
        foo(a)
        val a = 2

        block {
            foo(a)

            val a = 3
            block {
                //Breakpoint!
                foo(a)
            }
        }
    }
}

inline fun block(block: () -> Unit) {
    block()
}

fun foo(foo: Int) {}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME