package library

interface <lineMarker descr="*">My</lineMarker> {
    fun <lineMarker descr="*">foo</lineMarker>(): Int

    val <lineMarker descr="*">s</lineMarker>: String
}

class Your : My {
    override fun <lineMarker descr="*">foo</lineMarker>(): Int {
        return 42
    }

    override val <lineMarker descr="*">s</lineMarker>: String
        get() = ""
}