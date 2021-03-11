class Usages {
    void foo() {
        Foo.foo(2);
    }

    void fooX() {
        Foo.foo(1, 2);
    }

    void fooXY() {
        Foo.foo(1, 1.0, 2);
    }

    void fooXYZ() {
        Foo.foo(1, 1.0, "1", 2);
    }
}