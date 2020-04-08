interface <lineMarker>A1</lineMarker> {
    fun <lineMarker>foo</lineMarker>()
}

interface <lineMarker>B1</lineMarker> {
    fun <lineMarker>foo</lineMarker>()
}

class C1: A1, B1 {
    override fun <lineMarker descr="Implements function in 'A1'<br/>Implements function in 'B1'">foo</lineMarker>() {}
}

/*
LINEMARKER: Implements function in 'A1'<br/>Implements function in 'B1'
TARGETS:
NavigateToSeveralSuperElements.kt
    fun <1>foo()
}

interface B1 {
    fun <2>foo()
*/