class Base {
    protected void foo(){}
}

class Derived extends Base {
    @Override
    public void <caret>foo() {
        super.foo();
    }
}
