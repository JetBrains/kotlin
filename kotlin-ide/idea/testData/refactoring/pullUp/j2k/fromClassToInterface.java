abstract class <caret>B implements A {
    // INFO: {"checked": "true"}
    static String X = "1" + "2";
    // INFO: {"checked": "true"}
    boolean foo(int n) {
        return n > 0;
    }
    // INFO: {"checked": "true"}
    static String foo2(int n) {
        return "_" + n + "_";
    }
    // INFO: {"checked": "true"}
    abstract int bar(String s);
    // INFO: {"checked": "true"}
    static class Y {

    }
}
