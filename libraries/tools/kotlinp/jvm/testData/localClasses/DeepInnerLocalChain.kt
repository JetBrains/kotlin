
fun test() {
    class Local {
        inner class Inner {
            val prop = object {
                fun foo() {
                    fun bar() {
                        class DeepLocal {
                            inner class Deepest {
                                fun local(): Local = Local()
                                fun inner(): Inner = Inner()
                                fun deep(): DeepLocal = DeepLocal()
                                fun deepest(): Deepest? = Deepest()
                            }
                        }
                    }
                }
            }
        }
    }
}
