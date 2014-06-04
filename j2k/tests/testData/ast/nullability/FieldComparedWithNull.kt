class C() {
    private var s: String? = ""

    fun foo() {
        if (s == null) {
            System.out.print("null")
        }
    }
}