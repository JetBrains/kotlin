class Outer {
    private class Nested1() {

        public constructor(a: Int) : this() {
        }

        protected constructor(c: Char) : this() {
        }

        private constructor(b: Boolean) : this() {
        }
    }

    protected class Nested2() {

        public constructor(a: Int) : this() {
        }

        protected constructor(c: Char) : this() {
        }

        private constructor(b: Boolean) : this() {
        }

    }

    class Nested3() {

        public constructor(a: Int) : this() {
        }

        protected constructor(c: Char) : this() {
        }

        private constructor(b: Boolean) : this() {
        }
    }

    public class Nested4() {

        public constructor(a: Int) : this() {
        }

        protected constructor(c: Char) : this() {
        }

        private constructor(b: Boolean) : this() {
        }
    }

    default object {

        fun foo() {
            val nested1 = Nested1(1)
            val nested2 = Nested2(2)
            val nested3 = Nested3(3)
            val nested4 = Nested4(4)
        }
    }
}
