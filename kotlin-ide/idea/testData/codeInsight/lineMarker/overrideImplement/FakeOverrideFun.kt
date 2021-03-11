interface <lineMarker>A</lineMarker> {
  fun <lineMarker>f</lineMarker>() {}
}

interface <lineMarker>B</lineMarker> : A

interface <lineMarker>C</lineMarker> : B, A

class SomeClass() : C {
  override fun <lineMarker descr="Overrides function in 'A'">f</lineMarker>() {}
}