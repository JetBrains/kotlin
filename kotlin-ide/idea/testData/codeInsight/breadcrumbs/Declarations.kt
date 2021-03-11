class Outer {
    companion object {
        class SomeClass : java.io.Serializable {
            companion object CompanionName {
                private object SomeObject {
                    fun String.someFun(p: Int, b: Boolean): String {
                        fun localFun() {
                            val v = doIt(fun (x: Int, y: Char) {
                                <caret>
                            })
                        }
                    }
                }
            }
        }
    }
}
