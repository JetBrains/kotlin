// WITH_RUNTIME
fun test() {
    val map = mapOf(1 to "two")
    for (<caret>(key, value) in map) {
    }
}