class C(private var s: String?) {
    {
        if (s == null) {
            System.out.print("null")
        }
    }
}