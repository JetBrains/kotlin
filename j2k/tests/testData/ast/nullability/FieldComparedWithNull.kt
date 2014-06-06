class C() {
    private var s: String? = x()

    fun foo() {
        if (s == null) {
            System.out.print("null")
        }
    }
}