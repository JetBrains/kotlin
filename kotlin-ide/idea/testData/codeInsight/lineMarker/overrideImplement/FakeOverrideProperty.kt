interface <lineMarker>A</lineMarker> {
    val <lineMarker>f</lineMarker>: Int
        get() = 2
}

interface <lineMarker>B</lineMarker> : A

interface <lineMarker>C</lineMarker> : B, A

class SomeClass() : C {
    override val <lineMarker descr="Overrides property in 'A'">f</lineMarker>: Int = 4
}