class X extends A {
    @Override
    String foo(int n) {
        return "";
    }
}

class Y extends X {
    @Override
    String foo(int n) {
        return super.foo(n);
    }
}

class Test {
    void test() {
        new A().foo(1);
        new B().foo(2);
        new X().foo(3);
        new Y().foo(4);
    }
}