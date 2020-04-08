expect sealed class <lineMarker>Sealed</lineMarker> {

    object <lineMarker>Sealed1</lineMarker> : Sealed

    class <lineMarker>Sealed2</lineMarker> : Sealed {
        val <lineMarker>x</lineMarker>: Int
        fun <lineMarker>foo</lineMarker>(): String
    }
}
