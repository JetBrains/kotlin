package test3

import test.A

class D {
    class C {
        fun test() {
            A.B.X()

            A.B.Companion.Y()
            A.B.foo(A.B.bar)
            //1.extFoo(1.extBar) // conflict

            A.B.O.Y()
            A.B.O.foo(A.B.O.bar)

            with (A.B.O) {
                A.B.Companion.Y()
                foo(bar)
                1.extFoo(1.extBar)
            }
        }
    }

}