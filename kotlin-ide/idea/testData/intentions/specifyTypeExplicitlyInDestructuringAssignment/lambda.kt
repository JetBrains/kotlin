// WITH_RUNTIME
fun test() {
    val list = emptyList<Pair<Int, String>>()
    list.forEach { (i, s)<caret> ->
    }
}