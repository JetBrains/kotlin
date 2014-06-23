class C(private val s: String?) {

    fun foo() {
        if (s != null) {
            System.out.print("not null")
        }
    }
}