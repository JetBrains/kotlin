// FIX: Replace with 'getOrElse' call
// DISABLE-ERRORS
// WITH_RUNTIME
fun test(map: Map<Int, String>) {
    val s = map[1]<caret>!!
}