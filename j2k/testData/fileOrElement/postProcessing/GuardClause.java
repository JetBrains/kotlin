public class Test {
    void test(String s) {
        if (s == null) {
            throw new IllegalArgumentException("s should not be null");
        }
    }
}