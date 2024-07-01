fun box(stepId: Int, isWasm: Boolean): String {
    val expected = if (stepId == 5) stepId - 1 else stepId

    var got = simpleFunction()
    if (got != "$expected") return "Fail simpleFunction: '$got' != '$expected'"

    got = inlineFunctionProxy()
    if (got != "$expected") return "Fail inlineFunctionProxy: '$got' != '$expected'"

    got = SimpleChild().simpleMethod()
    if (got != "$expected") return "Fail SimpleChild.simpleMethod: '$got' != '$expected'"

    got = inlineChildProxy()
    if (got != "$expected") return "Fail inlineChildProxy: '$got' != '$expected'"

    return "OK"
}
