import java.lang.Override;

class J extends A {
    private int p;

    public J() {
        super(0);
    }

    @Override
    public int getP() {
        return p;
    }

    @Override
    public void setP(int value) {
        p = value;
    }
}

class Test {
    static void test() {
        new A(0).getP();
        new A(0).setP(1);

        new B(0).getP();
        new B(0).setP(2);

        new C().getP();
        new C().setP(3);

        new J().getP();
        new J().setP(4);

        new D().getP();
        new D().setP(5);

        new E().getP();
        new E().setP(6);
    }
}