open class <lineMarker>Base</lineMarker> {
  open var <lineMarker>writable</lineMarker>: Int = 12
}

class SubBase: Base() {
  override var <lineMarker>writable</lineMarker>: Int = 42
}