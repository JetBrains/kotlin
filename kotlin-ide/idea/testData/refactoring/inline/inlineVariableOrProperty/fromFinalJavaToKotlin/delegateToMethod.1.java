public final class JavaClass {
    public Integer <caret>field = otherMethod();

    public Integer otherMethod() {
        return 42;
    }
}