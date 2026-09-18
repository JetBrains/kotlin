import sample.*

fun box(stepId: Int, isWasm: Boolean): String {
    val fromLibResult = runCoroutine(fromLib("O"))
    val fromMainResult = runCoroutine(fromMain("K"))
    return "$fromLibResult$fromMainResult"
}
