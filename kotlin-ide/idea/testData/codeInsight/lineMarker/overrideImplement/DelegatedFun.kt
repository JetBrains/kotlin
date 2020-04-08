interface <lineMarker>A</lineMarker> {
    fun <lineMarker>f</lineMarker>() {
    }
}

interface <lineMarker>B</lineMarker> : A

open class <lineMarker>C</lineMarker>(b : B) : B by b, A {
}

class D(b : B) : C(b) {
  override fun <lineMarker descr="Overrides function in 'A'">f</lineMarker>() {}
}