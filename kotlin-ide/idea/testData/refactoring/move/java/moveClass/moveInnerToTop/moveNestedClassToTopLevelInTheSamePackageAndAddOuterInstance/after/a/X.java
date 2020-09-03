package a;

public class X {
    private final A outer;

    public X(A outer, String s) {
        this.outer = outer;
        System.out.println(s);
    }
}
