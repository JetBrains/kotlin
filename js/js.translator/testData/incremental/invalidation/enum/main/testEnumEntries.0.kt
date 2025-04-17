@kotlin.ExperimentalStdlibApi
fun testEnumEntries(stepId: Int): Boolean {
    when (stepId) {
        0, 1, 2, 3, 4, 5 -> return true
        else -> return false
    }
    return true
}
