public class ClassWithReferenceToInner {
    public String f1(Thread.State state) {
        return "OK"
    }

    public String f2(Outer.Nested nested) {
        return "OK"
    }
}

class Outer {
    class Nested {}
}
