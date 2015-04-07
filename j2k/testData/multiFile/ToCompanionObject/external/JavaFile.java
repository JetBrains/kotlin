package test;

class C {
    void foo(ClassWithStatics c) {
        ClassWithStatics.staticMethod(ClassWithStatics.staticField);
        c.instanceMethod();
        ClassWithStatics.staticField += 2;
    }
}

class D extends ClassWithStatics {
    void foo() {
        staticMethod(staticField);
        ourValue *= 2;
    }
}