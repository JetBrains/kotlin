class C {
    class object {
        private val staticField1 = 0
        private val staticField2 = 0

        fun create(p: Int): C {
            val __ = C()
            System.out.println(staticField1 + C.staticField2)
            return __
        }
    }
}