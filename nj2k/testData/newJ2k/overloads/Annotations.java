import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class A {
    @NotNull
    Object foo(String s) {
        System.out.println("s = " + s);
        return "";
    }

    @NotNull
    Object foo() {
        return foo(null);
    }

    @Nullable
    Object bar(String s) {
        System.out.println("s = " + s);
        return s == null ? "" : null;
    }

    @NotNull
    Object bar() {
        return bar(null);
    }

    public @Nullable Object bar1(String s) {
        System.out.println("s = " + s);
        return s == null ? "" : null;
    }

    public @NotNull Object bar1() {
        return bar1(null);
    }

    @Deprecated
    public void f() {
        f(1);
    }

    public void f(int p) {
        System.out.println("p = " + p);
    }
}