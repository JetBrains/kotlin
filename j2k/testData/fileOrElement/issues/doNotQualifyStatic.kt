class Outer {

    public class Nested {
        public fun foo() {
            o = null
        }
    }

    default object {
        public var o: Any? = Object()
    }
}