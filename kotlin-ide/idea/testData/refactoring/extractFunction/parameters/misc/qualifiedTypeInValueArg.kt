// SIBLING:
class MyClass {
    fun test() {
        <selection>val a: Any = P.Q()
        val t = P.R<P.Q>(a as P.Q)</selection>
    }

    public class P<T> {
        public class Q {

        }

        public class R<T>(val t: T) {

        }
    }
}