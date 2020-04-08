package noParameterLambdaArgumentCallInInline

fun main(args: Array<String>) {
    "123".lookAtMe {
        val c = "c"
    }
}

inline fun String.lookAtMe(f: String.() -> Unit) {
    val a = "a"
    //Breakpoint!
    f()
    val b = "b"
}