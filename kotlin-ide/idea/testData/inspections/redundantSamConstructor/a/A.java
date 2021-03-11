package a;

public class A<T> {
    public void foo(JFunction1<T> r) {}

    public static <T> JFunction1<T> expectedType(JFunction1<T> r) {
        return r;
    }
}