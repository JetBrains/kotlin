interface <caret>I {
    // INFO: {"checked": "true"}
    int x = 2 * 3;

    // INFO: {"checked": "true"}
    int bar(String s);

    // INFO: {"checked": "true"}
    class X {

    }
}