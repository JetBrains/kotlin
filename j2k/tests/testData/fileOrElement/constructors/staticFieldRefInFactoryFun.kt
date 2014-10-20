fun C(p: Int): C {
    val __ = C()
    System.out.println(C.staticField1 + C.staticField2)
    return __
}

class C {
    class object {
        private val staticField1 = 0
        private val staticField2 = 0
    }
}
