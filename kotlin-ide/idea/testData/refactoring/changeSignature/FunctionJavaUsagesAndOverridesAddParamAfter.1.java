import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class X extends A {
    @NotNull
    @Override
    String foo(int n, @NotNull String s, @Nullable Object o) {
        return "";
    }
}

class Y extends X {
    @NotNull
    @Override
    String foo(int n, @NotNull String s, @Nullable Object o) {
        return super.foo(n, s, o);
    }
}

class Test {
    void test() {
        new A().foo(1, "abc", "def");
        new B().foo(2, "abc", "def");
        new X().foo(3, "abc", "def");
        new Y().foo(4, "abc", "def");
    }
}