//file
// !OPEN_BY_DEFAULT: true

class A {
    void foo1() { }
    private void foo2(){}
    final void foo3(){}
}

final class B {
    void foo(){}
}

abstract class C {
    abstract void foo();
}

interface I{
    void foo();
}

class D implements I {
    @Override
    public void foo() { }
}

enum E {
    int foo() { return 0; }
}
