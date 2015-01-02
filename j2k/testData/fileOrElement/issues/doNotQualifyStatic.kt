class Outer {

    public class Nested {
        public fun foo() {
            o = null
        }
    }

    class object {
        public var o: Any? = Object()
    }
}