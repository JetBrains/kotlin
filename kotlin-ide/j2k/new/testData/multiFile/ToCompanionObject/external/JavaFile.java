package test;

class C {
    void foo(ClassWithStatics c) {
        ClassWithStatics.staticMethod(ClassWithStatics.staticField);
        c.instanceMethod();
        ClassWithStatics.staticNonFinalField += 2;
    }

    void methodReferences() {
        Object staticMethod = ClassWithStatics::staticMethod;
        Object instanceMethod = ClassWithStatics::instanceMethod;
    }
}

class D extends ClassWithStatics {
    void foo() {
        staticMethod(staticField);
        ourValue *= 2;
    }
}