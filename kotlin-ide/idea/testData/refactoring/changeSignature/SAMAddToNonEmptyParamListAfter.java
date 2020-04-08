interface SAM {
    String <caret>foo(String s, int n, Object o);
}

class JTest {
    static void samTest(SAM sam) { }
}