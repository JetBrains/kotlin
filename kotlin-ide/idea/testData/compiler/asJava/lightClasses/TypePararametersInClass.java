public abstract class A <T extends A<T>> extends B<java.util.Collection<? extends T>> implements C<T> {
    public A() { /* compiled code */ }

    public class Inner <D> extends B<java.util.Collection<? extends T>> implements C<D> {
        public Inner() { /* compiled code */ }
    }

    public final class Inner2 <X> extends A<T>.Inner<X> implements C<X> {
        public Inner2() { /* compiled code */ }
    }
}
