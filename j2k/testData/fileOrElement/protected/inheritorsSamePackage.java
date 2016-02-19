package test;

public class BaseInheritorSamePackage {
    protected BaseInheritorSamePackage() {

    }

    protected void foo() {

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