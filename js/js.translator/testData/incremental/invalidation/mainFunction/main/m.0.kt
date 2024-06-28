var ok = "Fail"
fun main() {
   ok = "OK"
}
fun box(stepId: Int): String {
    return when (stepId) {
        0 -> ok
        else -> "Unknown"
    }
}
