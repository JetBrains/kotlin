// WITH_RUNTIME

fun foo(map: Map<Int, Int>) {
    for (entry<caret> in map.entries) {
        val (key) = entry
    }
}