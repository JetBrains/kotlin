internal class C(x: Any?, b: Boolean) {
    var x: Any? = null

    init {
        if (b) {
            this.x = x
        }
    }
}