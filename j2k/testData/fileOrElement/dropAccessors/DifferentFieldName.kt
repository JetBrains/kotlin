public class AAA {
    public var x: Int = 42
        private set

    public fun foo(other: AAA) {
        println(x)
        println(other.x)
        x = 10
    }
}
