// "Remove final upper bound" "true"

data class DC(val x: Int, val y: String) {
    fun <S : Int<caret>> foo(b: S) {
        val a: S = b
    }
}