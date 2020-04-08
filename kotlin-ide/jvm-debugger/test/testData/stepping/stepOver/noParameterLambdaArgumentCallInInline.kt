package noParameterLambdaArgumentCallInInline

/*
 KT-6477 Breakpoints do not work in inline functions at line where inlined argument without arguments is invoked
 */

fun main(args: Array<String>) {
    lookAtMe {
        val c = "c"
    }
}

inline fun lookAtMe(f: () -> Unit) {
    val a = "a"
    //Breakpoint!
    f()
    val b = "b"
}