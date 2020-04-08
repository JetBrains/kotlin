// WITH_RUNTIME

fun test() {
    val pair: Pair<List<Int>, List<Int>> = listOf(1 to 2, 3 to 4).<caret>filter { it.first > 1 }.filter { it.second > 2 }.unzip()
}