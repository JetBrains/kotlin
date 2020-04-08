import org.jetbrains.annotations.Nullable;

//file
class A {
    int field = foo();

    @Nullable int foo() { return 1; }
}
