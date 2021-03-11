public class J extends A {
    @Override
    public int getP() {
        return 1;
    }

    @Override
    public void setP(int value) {

    }
}

class Test {
    static void test() {
        new A().getP();
        new A().setP(1);

        new AA().getP();
        new AA().setP(1);

        new J().getP();
        new J().setP(1);

        new B().getP();
        new B().setP(1);
    }
}