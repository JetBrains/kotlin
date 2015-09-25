internal object Outer {
    var o: Any? = Object()

    class Nested {
        fun foo() {
            o = null
        }
    }
}
