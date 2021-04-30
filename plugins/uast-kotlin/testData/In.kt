// !IGNORE_FIR

fun foo(): Boolean {
    val x = 1
    val y = 10
    return x in 0..5 && y !in 4..9
}
