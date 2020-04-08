interface <lineMarker>I</lineMarker> {
    val <lineMarker>name</lineMarker>: String
}

class D(
    override val <lineMarker descr="Implements property in 'I'">name</lineMarker>: String
): I
