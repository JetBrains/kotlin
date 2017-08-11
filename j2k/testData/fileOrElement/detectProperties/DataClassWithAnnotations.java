@interface TestAnnotation {
}

public class Test {
    @TestAnnotation
    private String arg;

    public Test(@TestAnnotation String arg) {
        this.arg = arg;
    }

    @TestAnnotation
    public String getArg() {
        return arg;
    }

    @TestAnnotation
    public void setArg(String arg) {
        this.arg = arg;
    }
}
