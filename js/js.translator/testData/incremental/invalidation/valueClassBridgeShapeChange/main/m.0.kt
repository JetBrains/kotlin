fun box(stepId: Int, isWasm: Boolean): String {
    val actual = readToken(makeToken(41))
    return if (actual == 41) "OK" else "Fail step0: $actual"
}
