package test

fun main(args: Array<String>) {
    lookAtMe {
        val c = "c"
    }
}

inline fun lookAtMe(f: String.() -> Unit) {
    val a = "a"

    //Breakpoint!
    "123".
        f()
    val b = "b"
}

// STEP_OVER: 3