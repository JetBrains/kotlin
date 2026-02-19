var ok = "Fail"
fun main() {
   ok = "OK"
}
fun box(stepId: Int, isWasm: Boolean): String {
    return when (stepId) {
        0 -> ok
        else -> "Unknown"
    }
}
