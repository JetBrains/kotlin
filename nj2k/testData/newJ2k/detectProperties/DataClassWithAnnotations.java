@interface TestAnnotationField {}
@interface TestAnnotationParam {}
@interface TestAnnotationGet {}
@interface TestAnnotationSet {}


public class Test {
    @TestAnnotationField
    private String arg;

    public Test(@TestAnnotationParam String arg) {
        this.arg = arg;
    }

    @TestAnnotationGet
    public String getArg() {
        return arg;
    }

    @TestAnnotationSet
    public void setArg(String arg) {
        this.arg = arg;
    }
}
