public class AAA {
    public var x: Int = 42
        protected set

    public fun foo(other: AAA) {
        System.out.println(x)
        System.out.println(other.x)
        x = 10
    }
}

class BBB : AAA() {
    fun bar() {
        System.out.println(x)
        x = 10
    }
}