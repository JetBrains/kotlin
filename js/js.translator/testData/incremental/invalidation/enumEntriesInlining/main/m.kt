@kotlin.ExperimentalStdlibApi
fun box(stepId: Int, isWasm: Boolean): String {
    when {
        !testEnumEntries(stepId) -> return "Fail testEnumEntries"
    }
    return "OK"
}
