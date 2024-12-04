class ClassA : InterfaceA {
    override fun functionA(x: Int, s: String, b: Boolean?, i: Int): Int {
        return i - 1 + x + s.length + if (b == true) 1 else 0
    }
}
