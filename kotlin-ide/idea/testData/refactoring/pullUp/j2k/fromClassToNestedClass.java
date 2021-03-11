abstract class <caret>B extends T.U.A {
    // INFO: {"checked": "true"}
    int x = 2*3;
    // INFO: {"checked": "true"}
    static String X = "1" + "2";
    // INFO: {"checked": "true"}
    boolean foo(int n) {
        return n > 0;
    }
    // INFO: {"checked": "true"}
    static String foo2(int n) {
        return "_" + n + "_";
    }
    // INFO: {"checked": "true"}
    abstract int bar(String s);
    // INFO: {"checked": "true"}
    class X {

    }
    // INFO: {"checked": "true"}
    static class Y {

    }
}

class Test {
    static void test() {
        B b = new B() {
            public int bar(String s) {
                return s.length();
            }
        };
        int t1 = b.x;
        b.x = t1 + 1;
        String t2 = b.X;
        String t3 = B.X;
        b.foo(1);
        b.foo2(2);
        B.foo2(3);
        b.new X();
        new B.Y();
    }
}