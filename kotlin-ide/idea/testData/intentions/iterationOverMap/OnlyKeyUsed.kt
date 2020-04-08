// WITH_RUNTIME

fun main(args: Array<String>) {
    val map = hashMapOf(1 to 1)
    for (<caret>entry in map) {
        val key = entry.key
        val key2 = entry.key

        println(key)
    }
}