class TestInitializeInTry {
    internal var x: Any? = null
    private var y: Any? = null

    init {
        try {
            x = Any()
            y = Any()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        x.toString()
        y.toString()
    }
}
