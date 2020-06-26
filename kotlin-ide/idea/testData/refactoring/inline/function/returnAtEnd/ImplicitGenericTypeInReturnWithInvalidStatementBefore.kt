fun <T> buildList(size: Int) = listOf<T>()

fun f() {
    buildMyList<caret>(5)
}

private fun buildMyList(count: Int): List<String> {
    buildList(size = count)
    return buildList(size = count)
}
