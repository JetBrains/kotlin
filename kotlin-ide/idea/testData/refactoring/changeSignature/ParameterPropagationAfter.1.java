import org.jetbrains.annotations.NotNull;

class J extends A {
    @Override
    public void foo(int n, @NotNull String s) {

    }

    @Override
    public void bar(boolean b, int n, String s) {
        foo(1, "abc"); // Propagated parameters are not passed to calles in overriding methods
    }

    @Override
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

        new J().foo(1, "abc");
        new J().bar(true, 1, "abc");
        new J().baz();
    }
}