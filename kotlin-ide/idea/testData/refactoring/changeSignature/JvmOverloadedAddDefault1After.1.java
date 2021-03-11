class Usages {
    void foo() {
        Foo.foo();
    }

    void fooX() {
        Foo.foo(2, 1);
    }

    void fooXY() {
        Foo.foo(2, 1, 1.0);
    }

    void fooXYZ() {
        Foo.foo(2, 1, 1.0, "1");
    }
}