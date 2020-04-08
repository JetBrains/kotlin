interface SAM {
    String <caret>foo(String p, int n);
}

class JTest {
    static void samTest(SAM sam) { }
}