// ERROR: 'return' is not allowed here
// ERROR: Type mismatch: inferred type is String but Unit was expected
// ERROR: 'return' is not allowed here
// ERROR: Type mismatch: inferred type is String but Unit was expected
// ERROR: 'return' is not allowed here
// ERROR: Type mismatch: inferred type is String but Unit was expected
class Java8Class {
    fun foo0(r: Function0<String>) {}

    fun foo1(r: Function1<Int, String>) {}

    fun foo2(r: Function2<Int, Int, String>) {}

    fun helper() {}

    fun foo() {
        foo0 { "42" }
        foo0 { "42" }
        foo0 {
            helper()
            "42"
        }

        foo1 { i -> "42" }
        foo1 { i -> "42" }
        foo1 { i: Int ->
            helper()
            if (i > 1) {
                return@foo1 "42"
            }

            "43"
        }

        foo2 { i, j -> "42" }
        foo2 { i: Int, j: Int ->
            helper()
            "42"
        }

        val f = { i: Int, k: Int ->
            helper()
            if (i > 1) {
                return "42"
            }

            "43"
        }

        val f1 = { i1: Int, k1: Int ->
            val f2 = { i2: Int, k2: Int ->
                helper()
                if (i2 > 1) {
                    return "42"
                }

                "43"
            }
            if (i1 > 1) {
                return f.invoke(i1, k1)
            }
            f.invoke(i1, k1)
        }

        val runnable = { }

        foo1 { i: Int ->
            if (i > 1) {
                return@foo1 "42"
            }

            foo0 {
                if (true) {
                    return@foo0 "42"
                }
                "43"
            }

            "43"
        }
    }
}