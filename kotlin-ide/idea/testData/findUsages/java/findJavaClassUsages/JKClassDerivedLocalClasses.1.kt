fun foo() {
    public open class X: A()

    public interface T: A

    fun bar() {
        public open class Y: X()

        public class Z: Y(), T
    }
}