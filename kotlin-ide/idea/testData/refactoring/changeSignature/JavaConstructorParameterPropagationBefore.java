class A {
    public <caret>A() {

    }

    public A(boolean b) {
        this();
    }
}

class B extends A {
    public B() {

    }

    public B(boolean b) {
        super();
    }
}

class Test {
    void test() {
        new A();
        new A(true);
        new B();
        new B(true);
        new C();
        new D();
        new D(true);
    }
}