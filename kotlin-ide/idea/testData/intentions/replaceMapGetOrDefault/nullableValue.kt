// IS_APPLICABLE: false
// RUNTIME_WITH_FULL_JDK
fun main() {
    val map = mapOf(1 to "", 2 to null)
    val b = map.getOrDefault<caret>(2, "bar") == null
}