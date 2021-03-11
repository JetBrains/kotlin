fun foo() {
    {
        val t = <selection>object {
            val a = 1
            fun b() = a + 1
            val c: Int
                get() = b() + 1
        }</selection>
    }

    {
        val v = object {
            val x = 1
            fun b() = x + 1
            val c: Int
                get() = b() + 1
        }
    }

    {
        val w = object {
            fun b() = a + 1
            val a = 1
            val c: Int
                get() = b() + 1
        }
    }
}
