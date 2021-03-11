class B implements A {
    public int foo() {
        return 2;
    }
}

interface C extends A {
    public int foo();
}

class D extends B implements C {
    public int foo() {
        return 4;
    }
}