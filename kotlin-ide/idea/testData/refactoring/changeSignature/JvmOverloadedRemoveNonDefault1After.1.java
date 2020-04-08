class Usages {
    void foo() {
        Foo.foo();
    }

    void fooY() {
        Foo.foo(1.0);
    }

    void fooYZ() {
        Foo.foo(1.0, "1");
    }
}