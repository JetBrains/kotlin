interface <lineMarker descr="*">A</lineMarker> {
    override fun <lineMarker descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;C</body></html>"><lineMarker descr="*">toString</lineMarker></lineMarker>() = "A"
}

abstract class <lineMarker descr="*">B</lineMarker> : A

class C : B() {
    override fun <lineMarker descr="Overrides function in 'A'">toString</lineMarker>() = "B"
}
