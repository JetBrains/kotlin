internal class Outer {
    private inner class Inner1() {

        constructor(a: Int) : this() {}

        protected constructor(c: Char) : this() {}

        private constructor(b: Boolean) : this() {}
    }

    protected inner class Inner2() {

        constructor(a: Int) : this() {}

        protected constructor(c: Char) : this() {}

        private constructor(b: Boolean) : this() {}

    }

    internal inner class Inner3() {

        constructor(a: Int) : this() {}

        protected constructor(c: Char) : this() {}

        private constructor(b: Boolean) : this() {}
    }

    inner class Inner4() {

        constructor(a: Int) : this() {}

        protected constructor(c: Char) : this() {}

        private constructor(b: Boolean) : this() {}
    }

    fun foo() {
        val inner1 = Inner1(1)
        val inner2 = Inner2(2)
        val inner3 = Inner3(3)
        val inner4 = Inner4(4)
    }
}
