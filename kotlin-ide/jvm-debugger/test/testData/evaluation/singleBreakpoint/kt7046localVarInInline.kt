package kt7046localVarInInline

fun main(args: Array<String>) {
    a(321) {
        it
    }
}

inline fun a(a: Int, cb: (Int) -> Unit) {
    val b = 123
    //Breakpoint!
    cb(a*b)
}

// EXPRESSION: a*b
// RESULT: 39483: I