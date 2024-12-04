@kotlin.ExperimentalStdlibApi
fun box(stepId: Int, isWasm: Boolean): String {
    when {
        !testEnumValues(stepId) -> return "Fail testEnumValues"
        !testEnumEntries(stepId) -> return "Fail testEnumEntries"
    }
    return "OK"
}
