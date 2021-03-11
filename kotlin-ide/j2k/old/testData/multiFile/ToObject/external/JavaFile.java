package test;

class C {
    void foo() {
        Utils.foo1(Utils.staticField);
        Utils.staticField += Utils.foo2();
        PureUtils.foo1(PureUtils.foo2())
    }
}
