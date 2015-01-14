public class AAA {
    public var x: Int = 42
        private set

    public fun foo(other: AAA) {
        System.out.println(x)
        System.out.println(other.x)
        x = 10
    }
}
