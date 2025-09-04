fun box(stepId: Int, isWasm: Boolean): String {
    val expectCommon = if (stepId < 2) 0 else stepId
    var got = commonWrapper()
    if (expectCommon != got) {
        return "Fail commonWrapper(): $expectCommon != $got"
    }

    val expectJs = if (stepId > 1) 1 else stepId
    got = jsWrapper()
    if (expectJs != got) {
        return "Fail jsWrapper(): $expectJs != $got"
    }

    return "OK"
}
