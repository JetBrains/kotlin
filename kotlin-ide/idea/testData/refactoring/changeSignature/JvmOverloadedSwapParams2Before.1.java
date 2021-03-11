class Usages {
    void foo() {
        Foo.foo(1);
    }

    void fooY() {
        Foo.foo(1, 1.0);
    }

    void fooYZ() {
        Foo.foo(1, 1.0, "1");
    }
}