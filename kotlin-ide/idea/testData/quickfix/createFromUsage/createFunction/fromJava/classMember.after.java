// "Add method 'foo' to 'K'" "true"
class J {
    void test(K k) {
        boolean b = k.<caret>foo(1, "2");
    }
}