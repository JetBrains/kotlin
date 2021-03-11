// DISABLE-ERRORS
fun foo() {
    {
        <selection>object A {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }</selection>
    }

    {
        class B {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        object C {
            val x = 1
            fun b() = x + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        object D {
            fun b() = a + 1
            val a = 1
            val c: Int
                get() = b() + 1
        }
    }
}
