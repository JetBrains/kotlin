package a;

class J {
    void bar() {
        b.DependencyKt.getTest(new Test());
        b.DependencyKt.setTest(new Test(), 0);
    }
}
