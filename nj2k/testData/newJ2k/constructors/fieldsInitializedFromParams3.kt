internal class C(p: Int) {
    private val p: Int

    init {
        var p = p
        this.p = p
        println(p++)
        println(p)
    }
}