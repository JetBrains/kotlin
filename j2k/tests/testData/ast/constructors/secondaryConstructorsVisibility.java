class A {
    public A() {}

    public A(int a) { this(); }

    protected A(char c) { this(); }

    A(float f) { this(); }

    private A(double d) { this(); }
}

public class B {
    public B() {}

    public B(int a) { this(); }

    protected B(char c) { this(); }

    B(float f) { this(); }

    private B(double d) { this(); }
}