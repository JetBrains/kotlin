class C(p: Int) {
    private val p: Int

    {
        this.p = p
        System.out.println(p++)
        System.out.println(p)
    }
}