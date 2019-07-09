package test;

class BaseSuperSamePackage {
    public void usage1() {
        DerivedSuperSamePackage derived = new DerivedSuperSamePackage();
        derived.foo();
        int i = derived.i;
    }
}

class DerivedSuperSamePackage  extends BaseSuperSamePackage {
    protected DerivedSuperSamePackage() {

    }

    protected void foo() {

    }

    protected int i = 1;
}