fun testToplevelProperties(): Int {
    return globalValWrapper().toInt() +
            inlineGlobalValWrapper().toInt()
}
