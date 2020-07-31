class Java8Class {
    fun foo0(r: Function0<String?>?) {
        TODO("_root_ide_package_")
    }

    fun foo1(r: Function1<Int, String?>?) {
        TODO("_root_ide_package_")
    }

    fun foo2(r: Function2<Int?, Int?, String?>?) {
        TODO("_root_ide_package_")
    }

    fun helper() {
        TODO("_root_ide_package_")
    }

    fun foo() {
        foo0 { "42" }
        foo0 { "42" }
        foo0 {
            helper()
            "42"
        }
        foo1 { i: Int? -> "42" }
        foo1 { i: Int? -> "42" }
        foo1 { i: Int ->
            helper()
            if (i > 1) {
                return@foo1 null
            }
            "43"
        }
        foo2 { i: Int?, j: Int? -> "42" }
        foo2 { i: Int?, j: Int? ->
            helper()
            "42"
        }
        val f: Function2<Int, Int, String> = label@{ i: Int, k: Int? ->
            helper()
            if (i > 1) {
                return@label "42"
            }
            "43"
        }
        val f1 = label@{ i1: Int, k1: Int ->
            val f2: Function2<Int, Int, String> = label@{ i2: Int, k2: Int? ->
                helper()
                if (i2 > 1) {
                    return@label "42"
                }
                "43"
            }
            if (i1 > 1) {
                return@label f.invoke(i1, k1)
            }
            f.invoke(i1, k1)
        }
        val runnable = Runnable {}
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
