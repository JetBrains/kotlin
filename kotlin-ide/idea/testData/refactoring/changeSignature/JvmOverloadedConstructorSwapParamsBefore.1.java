class Usages {
    void foo() {
        new A();
    }

    void fooX() {
        new A(1);
    }

    void fooXY() {
        new A(1, 1.0);
    }

    void fooXYZ() {
        new A(1, 1.0, "1");
    }
}