public final class JavaClass {
    public static Integer <caret>field = staticMethod();

    public static Integer staticMethod() {
        return 42;
    }
}