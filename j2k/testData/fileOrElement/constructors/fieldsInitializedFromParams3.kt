class C(p: Int) {
    private val p: Int

    init {
        var p = p
        this.p = p
        System.out.println(p++)
        System.out.println(p)
    }
}