// IGNORE_FIR
// WITH_RUNTIME

object Holder {
    class Array
}

fun getEntry() : Map.Entry<kotlin.Array<String>, Holder.Array> {
    throw Error()
}

val <caret>x = getEntry()
