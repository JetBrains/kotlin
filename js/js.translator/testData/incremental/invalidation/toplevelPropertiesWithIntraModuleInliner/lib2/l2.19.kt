fun testToplevelProperties(): Int {
    return globalValWrapper().toInt() +
            inlineGlobalVarWrapper().toInt() +
            8.inlineExtensionPropertyWrapper().toInt()
}
