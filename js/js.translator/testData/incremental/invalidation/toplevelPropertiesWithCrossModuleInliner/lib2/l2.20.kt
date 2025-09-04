fun testToplevelProperties(): Int {
    return inlineGlobalValWrapper().toInt() +
            14.inlineExtensionPropertyWrapper().toInt()
}
