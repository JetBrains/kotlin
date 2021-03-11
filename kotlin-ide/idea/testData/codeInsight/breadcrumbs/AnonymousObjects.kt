val v = foo(object : java.lang.Runnable {
    val v2 = object : A, B {
        fun f() {
            object : java.io.Serializable, XXX {
                var o = object {
                    override fun equals(other: Any?): Boolean {
                        <caret>
                    }
                }
            }
        }
    }
})
