// FLOW: OUT

class C {
    fun Int.outer() {
        fun String.stringExtensionFun() = this
        fun Int.intExtensionFun() = this
        fun C.cExtensionFun() = this

        fun String.foo() {
            val string = stringExtensionFun()
            val int = intExtensionFun()
            val c = cExtensionFun()
        }

        fun bar() {
            <caret>"A".foo()
        }
    }
}
