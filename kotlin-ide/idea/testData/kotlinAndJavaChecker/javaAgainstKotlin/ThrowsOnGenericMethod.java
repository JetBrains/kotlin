package test;
import foo.A;

public class ThrowsOnGenericMethod {
    public static void main(String[] args) {
        new A().<error descr="Unhandled exception: java.io.IOException">foo</error>("");
    }
}
