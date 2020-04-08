interface A {
    public void <caret>foo();
}

abstract class C implements B {
    @Override
    public void foo() {

    }
}