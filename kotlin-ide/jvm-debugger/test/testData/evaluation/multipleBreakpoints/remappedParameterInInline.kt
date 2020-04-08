package remappedParameterInInline

inline fun watch(p: String, f: (String) -> Int) {
    f(p)
}

inline fun inlineDefault(p: Int = 1, f: (Int) -> Unit) {
    // EXPRESSION: p
    // RESULT: 1: I
    //Breakpoint!
    f(p)
}

fun main(args: Array<String>) {
    val local = "mno"
    watch(local) { it.length }

    inlineDefault { it }
}
