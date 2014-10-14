public class AAA {
    private val x = 42
    private val other = AAA()

    public fun getX(): Int {
        return other.x
    }
}
