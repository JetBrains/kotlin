object Outer {
    public var o: Any? = Object()

    public class Nested {
        public fun foo() {
            o = null
        }
    }
}
