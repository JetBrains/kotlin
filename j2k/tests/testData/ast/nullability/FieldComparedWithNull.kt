class C {
    private val s = x()

    fun foo() {
        if (s == null) {
            System.out.print("null")
        }
    }
}