public final class JavaClass {
    public Integer <caret>field = staticMethod();

    public static Integer staticMethod() {
        return 42;
    }
}