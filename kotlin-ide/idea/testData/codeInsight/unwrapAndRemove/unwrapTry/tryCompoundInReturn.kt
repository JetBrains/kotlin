// IS_APPLICABLE: false
fun foo(n : Int): Int {
    return <caret>try {
        val m = n + 1
        m/0
    } catch (e: Exception) {
        -1
    }
}
