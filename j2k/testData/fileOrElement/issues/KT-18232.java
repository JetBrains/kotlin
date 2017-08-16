public class TestClass {
    private final String arg1;
    private final String arg2;

    public TestClass(String arg1, String arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @SomeAnnotation(annotationArg = "arg1")
    public String getArg1() {
        return arg1;
    }

    @SomeAnnotation
    public String getArg2() {
        return arg2;
    }
}

@interface SomeAnnotation{
    String annotationArg() default "";
}