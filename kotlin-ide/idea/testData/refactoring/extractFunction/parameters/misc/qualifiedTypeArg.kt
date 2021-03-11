// SIBLING:
class MyClass {
    fun test() {
        <selection>val t: P<P.Q>? = null</selection>
    }

    public class P<T> {
        public class Q {

        }
    }
}