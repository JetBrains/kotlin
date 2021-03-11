fun <caret>f(list: List<String>): Boolean {
    return list.any {
        if (it.isEmpty()) return@any false
        it.length < 10
    }
}

fun g() {
    println(f(listOf("a", "b")))
}