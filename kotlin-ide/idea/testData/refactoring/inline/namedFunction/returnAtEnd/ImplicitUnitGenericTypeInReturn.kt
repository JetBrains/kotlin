fun <T> buildType(size: Int): T = TODO()

fun f() {
    buildMyList<caret>(5)
}

private fun buildMyList(count: Int) {
    return buildType(size = count)
}