class A {
    public <caret>A(int n) {

    }

    public A(boolean b, int n) {
        this(n);
    }
}

class B extends A {
    public B(int n) {

        super(n);
    }

    public B(boolean b, int n) {
        super(n);
    }
}

class Test {
    void test(int n) {
        new A(n);
        new A(true, n);
        new B(n);
        new B(true, n);
        new C(n);
        new D(n);
        new D(true, n);
    }
}