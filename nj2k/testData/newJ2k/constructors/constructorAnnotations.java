import javaApi.Anon5;

class A {
    private final int a;
    private final int b;

    @Anon5(10)
    public A(int a, int b) {
        this.a = a;
        this.b = b;
    }

    @Deprecated // this constructor will not be replaced by default parameter value in primary because of this annotation
    public A(int a) {
        this(a, 1);
    }
}

class B {
    @Anon5(11)
    public B() {
    }
}

class C {
    @Anon5(12)
    private C() {
    }
}