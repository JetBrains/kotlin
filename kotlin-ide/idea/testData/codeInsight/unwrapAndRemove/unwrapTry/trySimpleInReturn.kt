// OPTION: 0
fun foo(n : Int): Int {
    return try {
        n/0<caret>
    } catch (e: Exception) {
        -1
    }
}
