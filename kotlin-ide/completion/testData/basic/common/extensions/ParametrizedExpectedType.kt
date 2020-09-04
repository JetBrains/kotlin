// FIR_COMPARISON
class Generic<T>

fun <A : String, B, C> Generic<A>.extension(arg: (A) -> B): B = TODO()

fun test() {
    val v = Generic<String>()
    v.ext<caret>
}

// EXIST: extension
