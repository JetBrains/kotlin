class bar() {
    operator fun invoke(i: Any?, j: Any?) : Boolean {
        return true
    }
}

fun foo(i: Any?, j: Any?) {
    val test = bar()<caret>(i, j)
}