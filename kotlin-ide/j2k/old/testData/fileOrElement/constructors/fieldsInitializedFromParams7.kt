// ERROR: Property must be initialized or be abstract
internal class C(x: Any, b: Boolean) {
    var x: Any

    init {
        if (b) {
            this.x = x
        }
    }
}