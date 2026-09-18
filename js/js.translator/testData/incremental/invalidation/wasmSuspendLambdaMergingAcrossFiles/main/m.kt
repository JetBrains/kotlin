fun box(stepId: Int, isWasm: Boolean): String {
    val fromA = runCoroutine(a("O"))
    val fromB = runCoroutine(b("K"))
    return "$fromA$fromB"
}
