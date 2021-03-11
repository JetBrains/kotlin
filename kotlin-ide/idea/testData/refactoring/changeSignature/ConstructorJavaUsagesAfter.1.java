class Foo extends A {
    Foo() {
        super(2, "abc");
    }

    void foo() {
        new A(1, "abc");
    }
}