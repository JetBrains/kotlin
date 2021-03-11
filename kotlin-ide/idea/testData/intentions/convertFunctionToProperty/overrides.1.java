import test.*;

class JA extends A {
    public JA() {
        super(1);
    }

    @Override
    boolean foo() {
        return true;
    }
}

class JTest {
    void test() {
        boolean x = new A(1).foo();
        boolean y = new JA().foo();
    }
}