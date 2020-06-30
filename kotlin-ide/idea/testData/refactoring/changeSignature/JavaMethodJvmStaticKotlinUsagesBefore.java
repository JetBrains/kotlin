public class JavaTest {
    public static void test() {
        Test.Companion.<caret>fooStatic("First", 2);
        Test.fooStatic("First", 2);
    }
}