class Usages {
    void foo() {
        Foo.foo();
    }

    void fooX() {
        Foo.foo();
    }

    void fooXY() {
        Foo.foo(1.0);
    }

    void fooXYZ() {
        Foo.foo(1.0, "1");
    }
}