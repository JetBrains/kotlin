package test;

public class BaseSamePackage {
    protected BaseSamePackage() {

    }

    protected void foo() {

    }

    protected int i = 1;
}

class DerivedSamePackage {
    public void usage1() {
        BaseSamePackage base = new BaseSamePackage();
        base.foo();
        int i = base.i;
    }
}
