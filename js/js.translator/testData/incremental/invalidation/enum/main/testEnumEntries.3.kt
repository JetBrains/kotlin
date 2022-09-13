@kotlin.ExperimentalStdlibApi
fun testEnumEntries(stepId: Int): Boolean {
    val entries = TestEnum.entries

    if (entries.contains((object {}) as Any?)) return false

    when (stepId) {
        0 -> if (entries.isNotEmpty()) return false
        1, 2, 3 -> {
            if (entries.size != 2) return false
            if (entries.indexOf(TestEnum.A) != 0) return false
            if (entries.indexOf(TestEnum.B) != 1) return false
        }
        else -> return false
    }

    return true
}
