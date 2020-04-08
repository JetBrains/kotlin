package test;

public class BaseInheritorSamePackage {
    protected BaseInheritorSamePackage() {

    }

    protected BaseInheritorSamePackage(int x) {

    }

    protected void foo() {
        new BaseInheritorSamePackage(1);
    }

    protected int i = 1;
}

class DerivedInheritorSamePackage  extends BaseInheritorSamePackage {
    public void usage1() {
        BaseInheritorSamePackage base = new BaseInheritorSamePackage();
        base.foo();
        int i = base.i;
    }
}