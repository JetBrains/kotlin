public class Outer {
    public open class X: A()

    public interface T: A

    public class Inner {
        public open class Y: X()

        public class Z: Y(), T
    }
}