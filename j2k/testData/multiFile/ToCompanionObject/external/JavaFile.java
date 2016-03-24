package test;

class C {
    void foo(ClassWithStatics c) {
        ClassWithStatics.staticMethod(ClassWithStatics.staticField);
        c.instanceMethod();
        ClassWithStatics.staticNonFinalField += 2;
    }

    void methodReferences() {
        JFunction1ReturnType<Integer> staticMethod = ClassWithStatics::staticMethod;
        JFunction1ReturnType<ClassWithStatics> instanceMethod = ClassWithStatics::instanceMethod;
    }
}

class D extends ClassWithStatics {
    void foo() {
        staticMethod(staticField);
        ourValue *= 2;
    }
}