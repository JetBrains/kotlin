public class JavaClass {
    public static Integer <caret>a() {
        return staticMethod();
    }

    public static Integer staticMethod() {
        return 42;
    }
}