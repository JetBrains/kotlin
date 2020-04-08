public class A {
    public interface I {
        void f();
    }
}

public class B extends A {
}

public class Test {
    public B.I z = null;
}