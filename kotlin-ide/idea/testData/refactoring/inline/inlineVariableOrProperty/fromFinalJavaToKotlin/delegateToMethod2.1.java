public final class JavaClass {
    public Integer <caret>field = this.otherMethod();

    public Integer otherMethod() {
        return 42;
    }
}