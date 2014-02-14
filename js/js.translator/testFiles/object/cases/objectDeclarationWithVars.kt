package foo

object State {
    val c = 2
    val b = 1
}

fun box(): Boolean {
    if (State.c != 2) return false
    if (State.b != 1) return false
    return true
}