// TODO: Declarations have no implementation and should be considered as "overloaded"
interface <lineMarker descr="*">First</lineMarker> {
    val <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>">some</lineMarker>: Int
    var <lineMarker descr="<html><body>Is implemented in <br/>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>">other</lineMarker>: String
        get
        set

    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;Second</body></html>">foo</lineMarker>()
}

interface Second : First {
    override val <lineMarker descr="Overrides property in 'First'">some</lineMarker>: Int
    override var <lineMarker descr="Overrides property in 'First'">other</lineMarker>: String
    override fun <lineMarker descr="Overrides function in 'First'">foo</lineMarker>()
}