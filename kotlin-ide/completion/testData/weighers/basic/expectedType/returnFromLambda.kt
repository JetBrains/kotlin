// FIR_COMPARISON

val a: Int
val b: String
val c: Long
val d: Int

fun test(): Int {
    receiveLambda {
        return@receiveLambda <caret>
    }
    return 2
}

fun receiveLambda(x: () -> Int){}

// ORDER: a, d, test