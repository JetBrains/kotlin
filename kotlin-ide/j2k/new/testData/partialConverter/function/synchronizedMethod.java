class A {
    synchronized void <caret>foo() {
        bar();
    }

    void bar() {
    }
}
