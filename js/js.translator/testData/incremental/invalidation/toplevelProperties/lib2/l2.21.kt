fun testToplevelProperties(): Int {
    return globalValWrapper().toInt() +
            inlineGlobalValWrapper().toInt() +
            globalVarWrapper().toInt() +
            inlineGlobalVarWrapper().toInt() + 6
}
