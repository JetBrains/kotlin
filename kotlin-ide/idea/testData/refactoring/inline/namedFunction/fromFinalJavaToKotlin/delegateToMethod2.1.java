public final class JavaClass {
    public Integer <caret>a() {
        return this.otherMethod();
    }

    public Integer otherMethod() {
        return 42;
    }
}