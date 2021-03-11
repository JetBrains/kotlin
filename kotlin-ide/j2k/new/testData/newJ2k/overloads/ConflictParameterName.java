public class Test {

    private int x = 1;

    private final boolean a = true;

    private final boolean b = true;

    private final boolean c() {
        return true;
    }

    private final boolean isD() {
        return true;
    }

    private final E e = new E();

    public static class E {
        public boolean ee = true;
    }

    public static class F {
        public boolean f = true;
    }

    private final int getG() { return 1; }

    public void foo() {
        foo(a, this.b, c(), isD(), e.ee, new F().f, getG());
    }

    public void foo(boolean a, boolean b, boolean c, boolean isD, boolean e, boolean f, int g) {
    }

    public void bar() {
        bar(a, a, b);
    }

    public void bar(boolean a, boolean e, boolean f) {
    }

    public void baz() {
        baz(!a, ++x, x++, x + x + 1);
    }

    public void baz(boolean a, int x, int y, int z) {
    }
}