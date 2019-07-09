internal object Outer {
    var o: Any? = Any()

    class Nested {
        fun foo() {
            o = null
        }
    }
}
