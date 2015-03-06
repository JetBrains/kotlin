class Outer {
    default object {

        private fun Nested1(a: Int): Nested1 {
            return Nested1()
        }

        private fun Nested1(c: Char): Nested1 {
            return Nested1()
        }

        private fun Nested1(b: Boolean): Nested1 {
            return Nested1()
        }

        private class Nested1


        protected fun Nested2(a: Int): Nested2 {
            return Nested2()
        }

        protected fun Nested2(c: Char): Nested2 {
            return Nested2()
        }

        private fun Nested2(b: Boolean): Nested2 {
            return Nested2()
        }

        protected class Nested2


        fun Nested3(a: Int): Nested3 {
            return Nested3()
        }

        fun Nested3(c: Char): Nested3 {
            return Nested3()
        }

        private fun Nested3(b: Boolean): Nested3 {
            return Nested3()
        }

        class Nested3


        public fun Nested4(a: Int): Nested4 {
            return Nested4()
        }

        public fun Nested4(c: Char): Nested4 {
            return Nested4()
        }

        private fun Nested4(b: Boolean): Nested4 {
            return Nested4()
        }

        public class Nested4

        fun foo() {
            val nested1 = Nested1(1)
            val nested2 = Nested2(2)
            val nested3 = Nested3(3)
            val nested4 = Nested4(4)
        }
    }
}