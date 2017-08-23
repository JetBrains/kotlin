public class Test {

    private final boolean isA = true;

    private final boolean isB = true;

    private final C c = new C();


    public void foo() {
        foo(isA, this.isB, c.isC, new D().isD);
    }

    public void foo(boolean isA, boolean isB, boolean isC, boolean isD) {
        System.out.println("isA=" + isA + ",isB=" + isB + "isC=" + isC + "isD=" + isD);
    }


    public static class C {
        public boolean isC = true;
    }

    public static class D {
        public boolean isD = true;
    }

}