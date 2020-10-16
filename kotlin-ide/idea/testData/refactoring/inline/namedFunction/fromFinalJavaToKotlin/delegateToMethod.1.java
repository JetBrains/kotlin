public final class JavaClass {
    public Integer <caret>a() {
        return otherMethod();
    }

    public Integer otherMethod() {
        return 42;
    }
}