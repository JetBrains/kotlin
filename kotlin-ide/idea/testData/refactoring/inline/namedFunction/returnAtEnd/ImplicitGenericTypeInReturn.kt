fun <T> buildList(size: Int) = listOf<T>()

fun f() {
    buildMyList<caret>(5)
}

private fun buildMyList(count: Int): List<String> {
    return buildList(size = count)
}
