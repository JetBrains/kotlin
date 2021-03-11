// DISABLE-ERRORS
fun foo() {
    {
        <selection>class A(p: Int = 1, val q: Int = p + 1) {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }</selection>
    }

    {
        class B(n: Int = 1, val q: Int = n + 1) {
            fun b() = a + 1
            val a = 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        class C {
            val x = 1
            fun b() = x + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        class D(n: Int = 1, val m: Int = n + 1) {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        class E(val n: Int = 1, val q: Int = n + 1) {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        object F {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }
    }
}
