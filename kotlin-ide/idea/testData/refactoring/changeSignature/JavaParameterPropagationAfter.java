class A {
    public void foo(int n, String s) {

    }

    public void bar(boolean b, int n, String s) {
        foo(n, s);
    }

    public void baz() {
        foo(1, "abc");
        bar(false, 1, "abc");
    }
}

class B extends A {
    public void foo(int n, String s) {

    }

    public void bar(boolean b, int n, String s) {
        foo(n, s);
    }

    public void baz() {
        foo(1, "abc");
        bar(false, 1, "abc");
    }
}

class Test {
    void test() {
        new A().foo(1, "abc");
        new A().bar(true, 1, "abc");
        new A().baz();

        new B().foo(1, "abc");
        new B().bar(true, 1, "abc");
        new B().baz();

        new C().foo(1, "abc");
        new C().bar(true, 1, "abc");
        new C().baz();
    }
}