import java.lang.Override;
import java.lang.Void;

class A {
    public void f1(){}
    public void f2(){}
    private void f3(){}
}

class B extends A {
    @Override
    public void f1() {
        super.f1();
    }
}

class C extends B {
    @Override
    public void f1() {
        super.f1();
    }
}

interface I {
    void f();
}

class D implements I {
    @Override
    public void f() { }
}

abstract class E {
    abstract void f1();
    void f2(){}
    void f3(){}
}

class F extends E {
    @Override
    void f1() {
    }

    @Override
    void f2() {
        super.f2();
    }
}
