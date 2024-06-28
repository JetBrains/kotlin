@kotlin.ExperimentalStdlibApi
fun testEnumEntries(stepId: Int): Boolean {
    when (stepId) {
        0, 1, 2, 3 -> return true
        else -> return false
    }
    return true
}
