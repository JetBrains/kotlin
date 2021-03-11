interface SAM {
    String <caret>foo(String s, int n);
}

class JTest {
    static void samTest(SAM sam) { }
}