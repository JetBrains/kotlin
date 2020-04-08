interface <lineMarker descr="*">A</lineMarker> {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;B</body></html>">foo</lineMarker>(str: String)
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;B</body></html>">foo</lineMarker>()
}

open class B : A {
    override fun <lineMarker descr="Implements function in 'A'">foo</lineMarker>(str: String) { }
    override fun <lineMarker descr="Implements function in 'A'">foo</lineMarker>() { }
}