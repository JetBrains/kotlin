import test.A;

class J extends A {
    boolean getFoo() {
        return true;
    }

    @Override
    int foo() {
        return 2;
    }
}