// SIBLING:
class MyClass {
    fun test() {
        <selection>val t: P.Q = P.Q()
        t</selection>
    }

    public class P {
        public class Q {

        }
    }
}