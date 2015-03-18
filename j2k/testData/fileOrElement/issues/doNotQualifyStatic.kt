class Outer {

    public class Nested {
        public fun foo() {
            o = null
        }
    }

    companion object {
        public var o: Any? = Object()
    }
}