import java.lang.Override;

public class J extends A {
    private int p;

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
        new A().getP();
        new A().setP(3);

        new AA().getP();
        new AA().setP(3);

        new J().getP();
        new J().setP(3);

        new B().getP();
        new B().setP(3);
    }
}