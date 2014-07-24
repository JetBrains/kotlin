class C(private val s: String?) {

    {
        if (s == null) {
            System.out.print("null")
        }
    }
}