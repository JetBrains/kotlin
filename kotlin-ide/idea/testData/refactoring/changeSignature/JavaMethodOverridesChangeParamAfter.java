import java.lang.Override;
import java.lang.String;

class A {
    String <caret>foo(int x) {
        return x.length() * 2;
    }
}

class B extends A {
    @Override
    String foo(int x) {
        return super.foo(x + "_");
    }
}

class Test {
    void test() {
        new A().foo("");
        new B().foo("");
        new X().foo("");
        new Y().foo("");
        new Z().foo("");
    }
}