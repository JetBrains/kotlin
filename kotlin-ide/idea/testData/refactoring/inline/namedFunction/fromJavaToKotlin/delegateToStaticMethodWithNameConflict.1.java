public class JavaClass {
    public Integer <caret>a() {
        return staticMethod();
    }

    public static Integer staticMethod() {
        return 42;
    }
}