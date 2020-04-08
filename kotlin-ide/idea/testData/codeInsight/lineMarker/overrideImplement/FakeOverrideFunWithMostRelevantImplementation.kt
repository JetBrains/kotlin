interface <lineMarker>A</lineMarker> {
    fun <lineMarker><lineMarker>f</lineMarker></lineMarker>() {}
}

interface <lineMarker>B</lineMarker> : A {
    override fun <lineMarker>f</lineMarker>() {}
}

interface <lineMarker>C</lineMarker> : B, A

class SomeClass() : C {
    override fun <lineMarker descr="Overrides function in 'B'">f</lineMarker>() {}
}