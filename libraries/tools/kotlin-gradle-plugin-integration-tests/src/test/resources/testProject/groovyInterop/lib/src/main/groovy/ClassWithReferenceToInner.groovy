public class ClassWithReferenceToInner {
    public void f1(Thread.State state) {}

    public void f2(Outer.Nested nested) {}
}

class Outer {
    class Nested {}
}
