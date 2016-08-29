import org.jetbrains.annotations.Nullable;

class A {
    void f(@Nullable String s) {
        System.out.println(s);
    }
}