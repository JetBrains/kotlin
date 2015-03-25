class C(private val s: String?) {

    init {
        if (s == null) {
            System.out.print("null")
        }
    }
}