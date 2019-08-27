public class TestInitInCtor {
    private int i;
    private int j;

    public TestInitInCtor(int i) {
        this.i = i;
        j = i;
    }

    public int foo() {
        return i + j;
    }
}