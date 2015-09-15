class Base {
    protected void foo(){}
}

class Derived extends Base {
    @Override
    public void foo() {
        super.foo();
    }
}
