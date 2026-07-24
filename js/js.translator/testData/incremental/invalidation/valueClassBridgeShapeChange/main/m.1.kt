fun box(stepId: Int, isWasm: Boolean): String {
    val actual = readToken(makeToken(41))
    return if (actual == 42) "OK" else "Fail step1: $actual"
}

