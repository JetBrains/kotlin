fun <T> buildList(size: Int) = listOf<T>()

class Test {
    fun f() {
        val list = buildMyList(5)
    }

    private fun buildMyList<caret>(count: Int): List<String> {
        return buildList(size = count)
    }
}