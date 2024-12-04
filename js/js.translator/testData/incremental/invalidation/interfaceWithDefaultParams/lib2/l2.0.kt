class ClassA : InterfaceA {
    override fun functionA(x: Int, s: String, b: Boolean?): Int {
        return x + s.length + if (b == true) 1 else 0
    }
}
